import type { APIGatewayProxyHandler } from 'aws-lambda';
import { GetCommand } from '@aws-sdk/lib-dynamodb';
import { ddb, TABLES } from '@/utils/dynamo';
import { ok, err } from '@/utils/response';
import { requireAuth } from '@/middleware/auth';

export const handler: APIGatewayProxyHandler = async (event) => {
  try {
    await requireAuth(event);
    const { clubId } = event.pathParameters ?? {};
    const res = await ddb.send(new GetCommand({ TableName: TABLES.CLUBS, Key: { id: clubId } }));
    if (!res.Item) return err('NOT_FOUND', 'Club not found', 404);
    return ok(res.Item);
  } catch (e: any) {
    if (e.message === 'UNAUTHORIZED') return err('UNAUTHORIZED', 'Authentication required', 401);
    return err('INTERNAL', e.message, 500);
  }
};
