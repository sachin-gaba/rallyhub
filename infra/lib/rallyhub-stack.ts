import { Stack, StackProps, RemovalPolicy, Duration, CfnOutput } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as lambda   from 'aws-cdk-lib/aws-lambda';
import * as apigw    from 'aws-cdk-lib/aws-apigateway';
import * as cognito  from 'aws-cdk-lib/aws-cognito';
import * as sns      from 'aws-cdk-lib/aws-sns';
import * as iam      from 'aws-cdk-lib/aws-iam';
import * as events   from 'aws-cdk-lib/aws-events';
import * as targets  from 'aws-cdk-lib/aws-events-targets';
import * as path     from 'path';

interface RallyhubStackProps extends StackProps { deployEnv: string; }

export class RallyhubStack extends Stack {
  constructor(scope: Construct, id: string, props: RallyhubStackProps) {
    super(scope, id, props);

    const isProd   = props.deployEnv === 'production';
    const removal  = isProd ? RemovalPolicy.RETAIN : RemovalPolicy.DESTROY;
    const AT = dynamodb.AttributeType.STRING;

    // ── Cognito ──────────────────────────────────────────────────
    const userPool = new cognito.UserPool(this, 'UserPool', {
      userPoolName:     `rallyhub-users-${props.deployEnv}`,
      selfSignUpEnabled: true,
      signInAliases:    { email: true },
      autoVerify:       { email: true },
      standardAttributes: { email: { required: true }, fullname: { required: true } },
      passwordPolicy:   { minLength: 8, requireUppercase: false, requireSymbols: false },
      mfa:              cognito.Mfa.OPTIONAL,
      removalPolicy:    removal,
    });
    const userPoolClient = new cognito.UserPoolClient(this, 'UserPoolClient', {
      userPool,
      authFlows:     { userSrp: true },
      generateSecret: false,
    });

    // ── DynamoDB tables ──────────────────────────────────────────
    const mkTable = (name: string, pk: string, sk?: string) => {
      const t = new dynamodb.Table(this, `Table-${name}`, {
        tableName:         `rallyhub-${name}`,
        partitionKey:      { name: pk, type: AT },
        ...(sk ? { sortKey: { name: sk, type: AT } } : {}),
        billingMode:       dynamodb.BillingMode.PAY_PER_REQUEST,
        encryption:        dynamodb.TableEncryption.AWS_MANAGED,
        pointInTimeRecovery: isProd,
        removalPolicy:     removal,
      });
      return t;
    };

    const tables = {
      users:        mkTable('users',        'id'),
      clubs:        mkTable('clubs',        'id'),
      memberships:  mkTable('memberships',  'userId',  'clubId'),
      sessions:     mkTable('sessions',     'id'),
      schedules:    mkTable('schedules',    'id'),
      ledger:       mkTable('ledger',       'id'),
      tournaments:  mkTable('tournaments',  'id'),
      declarations: mkTable('declarations', 'id'),
      payments:     mkTable('payments',     'id'),
      announcements:mkTable('announcements','id'),
      joinRequests: mkTable('join-requests','id'),
    };

    // ── DynamoDB GSIs ────────────────────────────────────────────
    // clubs: look up by invite code
    tables.clubs.addGlobalSecondaryIndex({
      indexName: 'inviteCode-index',
      partitionKey: { name: 'inviteCode', type: AT },
    });
    // memberships: list all members of a club
    tables.memberships.addGlobalSecondaryIndex({
      indexName: 'clubId-index',
      partitionKey: { name: 'clubId', type: AT },
    });
    // sessions: list sessions for a club
    tables.sessions.addGlobalSecondaryIndex({
      indexName: 'clubId-index',
      partitionKey: { name: 'clubId', type: AT },
    });
    // ledger: member's transaction history
    tables.ledger.addGlobalSecondaryIndex({
      indexName: 'userId-clubId-index',
      partitionKey: { name: 'userId', type: AT },
      sortKey:      { name: 'clubId', type: AT },
    });
    // tournaments: list for a club
    tables.tournaments.addGlobalSecondaryIndex({
      indexName: 'clubId-index',
      partitionKey: { name: 'clubId', type: AT },
    });
    // payments: pending payments per club
    tables.payments.addGlobalSecondaryIndex({
      indexName: 'clubId-status-index',
      partitionKey: { name: 'clubId', type: AT },
      sortKey:      { name: 'status', type: AT },
    });
    // announcements: list for a club
    tables.announcements.addGlobalSecondaryIndex({
      indexName: 'clubId-index',
      partitionKey: { name: 'clubId', type: AT },
    });
    // users: iCal token lookup
    tables.users.addGlobalSecondaryIndex({
      indexName: "icalToken-index",
      partitionKey: { name: "icalToken", type: AT },
    });

    // join-requests: pending for a club
    tables.joinRequests.addGlobalSecondaryIndex({
      indexName: 'clubId-index',
      partitionKey: { name: 'clubId', type: AT },
    });
    // schedules: list for a club
    tables.schedules.addGlobalSecondaryIndex({
      indexName: 'clubId-index',
      partitionKey: { name: 'clubId', type: AT },
    });

    // ── SNS push notification topic ──────────────────────────────
    const notificationTopic = new sns.Topic(this, 'NotificationTopic', {
      topicName: `rallyhub-notifications-${props.deployEnv}`,
    });

    // ── Lambda IAM role ──────────────────────────────────────────
    const lambdaRole = new iam.Role(this, 'LambdaRole', {
      assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole'),
      ],
    });
    Object.values(tables).forEach(t => t.grantReadWriteData(lambdaRole));
    notificationTopic.grantPublish(lambdaRole);
    // SES send permission
    lambdaRole.addToPolicy(new iam.PolicyStatement({
      actions:   ['ses:SendEmail'],
      resources: ['*'],
    }));

    // ── Common Lambda env vars ───────────────────────────────────
    const commonEnv: Record<string, string> = {
      COGNITO_USER_POOL_ID: userPool.userPoolId,
      COGNITO_CLIENT_ID:    userPoolClient.userPoolClientId,
      SNS_TOPIC_ARN:        notificationTopic.topicArn,
      AWS_REGION:           this.region,
      MICRONAUT_ENVIRONMENTS: props.deployEnv,
    };

    // ── GraalVM native Lambda factory ────────────────────────────
    // Runtime: PROVIDED_AL2023 — Lambda custom runtime
    // Handler: app.rallyhub.Handler
    // Code:    function.zip containing the 'bootstrap' native binary
    const nativeFn = (id: string, extraEnv: Record<string, string> = {}) =>
      new lambda.Function(this, id, {
        functionName:  `rallyhub-${id.toLowerCase()}-${props.deployEnv}`,
        runtime:       lambda.Runtime.PROVIDED_AL2023,   // Custom runtime for GraalVM
        handler:       'app.rallyhub.Handler',
        code:          lambda.Code.fromAsset(
                         path.join(__dirname, '../../apps/backend/build/function.zip')),
        role:          lambdaRole,
        memorySize:    256,       // Native is lean — 256MB is plenty
        timeout:       Duration.seconds(10),
        environment:   { ...commonEnv, ...extraEnv },
        // No SnapStart — GraalVM native eliminates cold starts (~50ms)
      });

    // ── Lambda functions ─────────────────────────────────────────
    const fns = {
      api: nativeFn('Api'),  // Single Lambda handles all routes via Micronaut routing
    };

    // ── API Gateway ──────────────────────────────────────────────
    const api = new apigw.RestApi(this, 'Api', {
      restApiName: `rallyhub-api-${props.deployEnv}`,
      defaultCorsPreflightOptions: {
        allowOrigins: apigw.Cors.ALL_ORIGINS,
        allowMethods: apigw.Cors.ALL_METHODS,
        allowHeaders: ['Content-Type', 'Authorization'],
      },
      deployOptions: {
        stageName: 'v1',
        throttlingBurstLimit: 100,
        throttlingRateLimit:  50,
      },
    });

    // Single Lambda proxy — Micronaut routes internally
    const integration = new apigw.LambdaIntegration(fns.api, { proxy: true });
    api.root.addProxy({ defaultIntegration: integration, anyMethod: true });

    // ── EventBridge: weekly session generation ───────────────────
    new events.Rule(this, 'WeeklySessionGen', {
      schedule:    events.Schedule.cron({ weekDay: 'MON', hour: '6', minute: '0' }),
      description: 'Generate upcoming sessions for all active schedules',
      targets:     [new targets.LambdaFunction(fns.api, {
        event: events.RuleTargetInput.fromObject({ source: 'scheduler', action: 'generateSessions' }),
      })],
    });

    // EventBridge: daily waitlist expiry check
    new events.Rule(this, 'DailyWaitlistExpiry', {
      schedule:    events.Schedule.cron({ hour: '7', minute: '0' }),
      description: 'Check and expire sequential waitlist windows',
      targets:     [new targets.LambdaFunction(fns.api, {
        event: events.RuleTargetInput.fromObject({ source: 'scheduler', action: 'expireWaitlistWindows' }),
      })],
    });

    // ── Outputs ──────────────────────────────────────────────────
    new CfnOutput(this, 'ApiUrl',           { value: api.url });
    new CfnOutput(this, 'UserPoolId',       { value: userPool.userPoolId });
    new CfnOutput(this, 'UserPoolClientId', { value: userPoolClient.userPoolClientId });
  }
}
