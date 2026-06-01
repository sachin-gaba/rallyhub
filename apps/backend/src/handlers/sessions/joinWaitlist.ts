import type { APIGatewayProxyHandler } from 'aws-lambda';
import { GetCommand, UpdateCommand } from '@aws-sdk/lib-dynamodb';
import { ddb, TABLES } from '@/utils/dynamo';
import { ok, err } from '@/utils/response';
import { requireAuth } from '@/middleware/auth';

export const handler: APIGatewayProxyHandler = async (event) => {
  try {
    const auth = await requireAuth(event);
    const { clubId, sessionId } = event.pathParameters ?? {};

    const sessionRes = await ddb.send(new GetCommand({ TableName: TABLES.SESSIONS, Key: { id: sessionId } }));
    const session = sessionRes.Item;
    if (!session) return err('NOT_FOUND', 'Session not found', 404);

    const waitlist: any[] = session.waitlist ?? [];
    if (waitlist.find((w) => w.userId === auth.userId)) return err('CONFLICT', 'Already on waitlist', 409);

    const position = waitlist.length + 1;
    const entry = { userId: auth.userId, joinedAt: new Date().toISOString(), position };

    await ddb.send(new UpdateCommand({
      TableName: TABLES.SESSIONS,
      Key: { id: sessionId },
      UpdateExpression: 'SET waitlist = list_append(if_not_exists(waitlist, :empty), :entry)',
      ExpressionAttributeValues: { ':entry': [entry], ':empty': [] },
    }));

    return ok({ position });
  } catch (e: any) {
    if (e.message === 'UNAUTHORIZED') return err('UNAUTHORIZED', 'Authentication required', 401);
    return err('INTERNAL', e.message, 500);
  }
};
