import { App } from 'aws-cdk-lib';
import { RallyhubStack } from './rallyhub-stack';

const app = new App();
const env = app.node.tryGetContext('env') ?? 'development';

new RallyhubStack(app, `RallyhubStack-${env}`, {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region:  process.env.CDK_DEFAULT_REGION ?? 'us-east-1',
  },
  deployEnv: env,
});
