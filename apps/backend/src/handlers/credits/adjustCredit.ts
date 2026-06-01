import type { APIGatewayProxyHandler } from 'aws-lambda';
import { GetCommand, PutCommand, UpdateCommand } from '@aws-sdk/lib-dynamodb';
import { v4 as uuid } from 'uuid';
import { ddb, TABLES } from '@/utils/dynamo';
import { ok, err } from '@/utils/response';
import { requireAuth } from '@/middleware/auth';
import { DEFAULT_NEGATIVE_CREDIT_LIMIT } from '@rallyhub/shared';

export const handler: APIGatewayProxyHandler = async (event) => {
  try {
    const auth = await requireAuth(event);
    const { clubId, userId } = event.pathParameters ?? {};
    const body = JSON.parse(event.body ?? '{}');
    const { amount, note } = body;
    if (typeof amount !== 'number') return err('VALIDATION', 'amount must be a number');

    // Check requester is organizer/co-organizer
    const requesterMembership = await ddb.send(new GetCommand({ TableName: TABLES.MEMBERSHIPS, Key: { userId: auth.userId, clubId } }));
    const role = requesterMembership.Item?.role ?? '';
    if (!['organizer_primary', 'organizer_additional', 'co_organizer'].includes(role))
      return err('FORBIDDEN', 'Only organizers can adjust credits', 403);

    const memberRes = await ddb.send(new GetCommand({ TableName: TABLES.MEMBERSHIPS, Key: { userId, clubId } }));
    if (!memberRes.Item) return err('NOT_FOUND', 'Member not found', 404);

    const newBalance = (memberRes.Item.creditBalance ?? 0) + amount;
    const now = new Date().toISOString();

    await Promise.all([
      ddb.send(new UpdateCommand({
        TableName: TABLES.MEMBERSHIPS,
        Key: { userId, clubId },
        UpdateExpression: 'SET creditBalance = :b',
        ExpressionAttributeValues: { ':b': newBalance },
      })),
      ddb.send(new PutCommand({
        TableName: TABLES.LEDGER,
        Item: { id: uuid(), userId, clubId, type: 'correction', amount, balanceAfter: newBalance, note, createdBy: auth.userId, createdAt: now },
      })),
    ]);

    return ok({ newBalance });
  } catch (e: any) {
    if (e.message === 'UNAUTHORIZED') return err('UNAUTHORIZED', 'Authentication required', 401);
    return err('INTERNAL', e.message, 500);
  }
};
