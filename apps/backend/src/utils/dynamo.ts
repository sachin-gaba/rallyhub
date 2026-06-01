import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb';

const client = new DynamoDBClient({ region: process.env.AWS_REGION ?? 'eu-west-1' });
export const ddb = DynamoDBDocumentClient.from(client);

export const TABLES = {
  USERS:        process.env.TABLE_USERS        ?? 'rallyhub-users',
  CLUBS:        process.env.TABLE_CLUBS        ?? 'rallyhub-clubs',
  MEMBERSHIPS:  process.env.TABLE_MEMBERSHIPS  ?? 'rallyhub-memberships',
  SESSIONS:     process.env.TABLE_SESSIONS     ?? 'rallyhub-sessions',
  LEDGER:       process.env.TABLE_LEDGER       ?? 'rallyhub-ledger',
  TOURNAMENTS:  process.env.TABLE_TOURNAMENTS  ?? 'rallyhub-tournaments',
  DECLARATIONS: process.env.TABLE_DECLARATIONS ?? 'rallyhub-declarations',
  PAYMENTS:     process.env.TABLE_PAYMENTS     ?? 'rallyhub-payments',
};
