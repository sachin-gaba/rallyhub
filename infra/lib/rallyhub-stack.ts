import { Stack, StackProps, RemovalPolicy, Duration, CfnOutput } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as cognito from 'aws-cdk-lib/aws-cognito';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as ses from 'aws-cdk-lib/aws-ses';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as path from 'path';

interface RallyhubStackProps extends StackProps {
  deployEnv: string;
}

export class RallyhubStack extends Stack {
  constructor(scope: Construct, id: string, props: RallyhubStackProps) {
    super(scope, id, props);

    const isProd = props.deployEnv === 'production';
    const removal = isProd ? RemovalPolicy.RETAIN : RemovalPolicy.DESTROY;

    // ─── Cognito ────────────────────────────────────────────────
    const userPool = new cognito.UserPool(this, 'UserPool', {
      userPoolName: `rallyhub-users-${props.deployEnv}`,
      selfSignUpEnabled: true,
      signInAliases: { email: true },
      autoVerify: { email: true },
      standardAttributes: { email: { required: true }, fullname: { required: true } },
      passwordPolicy: { minLength: 8, requireUppercase: false, requireSymbols: false },
      removalPolicy: removal,
      mfa: cognito.Mfa.OPTIONAL,
    });

    const userPoolClient = new cognito.UserPoolClient(this, 'UserPoolClient', {
      userPool,
      authFlows: { userSrp: true },
      generateSecret: false,
    });

    // ─── DynamoDB Tables ────────────────────────────────────────
    const tables: Record<string, dynamodb.Table> = {};

    const tableConfigs = [
      { name: 'users',        pk: 'id' },
      { name: 'clubs',        pk: 'id' },
      { name: 'memberships',  pk: 'userId',   sk: 'clubId' },
      { name: 'sessions',     pk: 'id' },
      { name: 'ledger',       pk: 'id' },
      { name: 'tournaments',  pk: 'id' },
      { name: 'declarations', pk: 'id' },
      { name: 'payments',     pk: 'id' },
      { name: 'schedules',    pk: 'id' },
    ];

    for (const cfg of tableConfigs) {
      tables[cfg.name] = new dynamodb.Table(this, `Table-${cfg.name}`, {
        tableName: `rallyhub-${cfg.name}`,
        partitionKey: { name: cfg.pk, type: dynamodb.AttributeType.STRING },
        ...(cfg.sk ? { sortKey: { name: cfg.sk, type: dynamodb.AttributeType.STRING } } : {}),
        billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
        encryption: dynamodb.TableEncryption.AWS_MANAGED,
        removalPolicy: removal,
        pointInTimeRecovery: isProd,
      });
    }

    // GSI: clubs by inviteCode
    tables['clubs'].addGlobalSecondaryIndex({
      indexName: 'inviteCode-index',
      partitionKey: { name: 'inviteCode', type: dynamodb.AttributeType.STRING },
    });

    // ─── SNS Platform App (push notifications) ──────────────────
    const notificationTopic = new sns.Topic(this, 'NotificationTopic', {
      topicName: `rallyhub-notifications-${props.deployEnv}`,
    });

    // ─── Lambda execution role ──────────────────────────────────
    const lambdaRole = new iam.Role(this, 'LambdaRole', {
      assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
      managedPolicies: [iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')],
    });
    Object.values(tables).forEach((t) => t.grantReadWriteData(lambdaRole));
    notificationTopic.grantPublish(lambdaRole);

    const commonEnv: Record<string, string> = {
      COGNITO_USER_POOL_ID: userPool.userPoolId,
      COGNITO_CLIENT_ID:    userPoolClient.userPoolClientId,
      SNS_TOPIC_ARN:        notificationTopic.topicArn,
      NODE_ENV:             props.deployEnv,
    };

    // ─── Lambda functions ───────────────────────────────────────
    const fnProps = (handler: string): lambda.FunctionProps => ({
      runtime: lambda.Runtime.NODEJS_20_X,
      code:    lambda.Code.fromAsset(path.join(__dirname, '../../apps/backend/dist')),
      handler,
      role: lambdaRole,
      timeout: Duration.seconds(30),
      memorySize: 256,
      environment: commonEnv,
    });

    const fns = {
      getClub:             new lambda.Function(this, 'GetClub',             fnProps('handlers/clubs/getClub.handler')),
      createClub:          new lambda.Function(this, 'CreateClub',          fnProps('handlers/clubs/createClub.handler')),
      bookSession:         new lambda.Function(this, 'BookSession',         fnProps('handlers/sessions/bookSession.handler')),
      joinWaitlist:        new lambda.Function(this, 'JoinWaitlist',        fnProps('handlers/sessions/joinWaitlist.handler')),
      adjustCredit:        new lambda.Function(this, 'AdjustCredit',        fnProps('handlers/credits/adjustCredit.handler')),
      createTournament:    new lambda.Function(this, 'CreateTournament',    fnProps('handlers/tournaments/createTournament.handler')),
      submitDeclaration:   new lambda.Function(this, 'SubmitDeclaration',   fnProps('handlers/members/submitHealthDeclaration.handler')),
    };

    // ─── API Gateway ────────────────────────────────────────────
    const api = new apigateway.RestApi(this, 'Api', {
      restApiName: `rallyhub-api-${props.deployEnv}`,
      defaultCorsPreflightOptions: {
        allowOrigins: apigateway.Cors.ALL_ORIGINS,
        allowMethods: apigateway.Cors.ALL_METHODS,
      },
    });

    const v1 = api.root.addResource('v1');

    // /v1/clubs
    const clubs = v1.addResource('clubs');
    clubs.addMethod('POST', new apigateway.LambdaIntegration(fns.createClub));
    const club = clubs.addResource('{clubId}');
    club.addMethod('GET', new apigateway.LambdaIntegration(fns.getClub));

    // /v1/clubs/{clubId}/sessions/{sessionId}/book
    const sessions  = club.addResource('sessions');
    const session   = sessions.addResource('{sessionId}');
    const bookRes   = session.addResource('book');
    bookRes.addMethod('POST', new apigateway.LambdaIntegration(fns.bookSession));
    session.addResource('waitlist').addMethod('POST', new apigateway.LambdaIntegration(fns.joinWaitlist));

    // /v1/clubs/{clubId}/credits/{userId}/adjust
    club.addResource('credits').addResource('{userId}').addResource('adjust').addMethod('POST', new apigateway.LambdaIntegration(fns.adjustCredit));

    // /v1/clubs/{clubId}/tournaments
    club.addResource('tournaments').addMethod('POST', new apigateway.LambdaIntegration(fns.createTournament));

    // /v1/clubs/{clubId}/health-declaration
    club.addResource('health-declaration').addMethod('POST', new apigateway.LambdaIntegration(fns.submitDeclaration));

    // ─── Outputs ────────────────────────────────────────────────
    new CfnOutput(this, 'ApiUrl',            { value: api.url });
    new CfnOutput(this, 'UserPoolId',        { value: userPool.userPoolId });
    new CfnOutput(this, 'UserPoolClientId',  { value: userPoolClient.userPoolClientId });
  }
}
