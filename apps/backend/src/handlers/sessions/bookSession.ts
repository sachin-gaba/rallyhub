import type { APIGatewayProxyHandler } from 'aws-lambda';
import { GetCommand, UpdateCommand } from '@aws-sdk/lib-dynamodb';
import { ddb, TABLES } from '@/utils/dynamo';
import { ok, err } from '@/utils/response';
import { requireAuth } from '@/middleware/auth';

export const handler: APIGatewayProxyHandler = async (event) => {
  try {
    const auth = await requireAuth(event);
    const { clubId, sessionId } = event.pathParameters ?? {};

    const [sessionRes, memberRes] = await Promise.all([
      ddb.send(new GetCommand({ TableName: TABLES.SESSIONS, Key: { id: sessionId } })),
      ddb.send(new GetCommand({ TableName: TABLES.MEMBERSHIPS, Key: { userId: auth.userId, clubId } })),
    ]);

    const session    = sessionRes.Item;
    const membership = memberRes.Item;

    if (!session)    return err('NOT_FOUND', 'Session not found', 404);
    if (!membership) return err('FORBIDDEN', 'Not a member of this club', 403);
    if (membership.role === 'inductee') return err('FORBIDDEN', 'Inductees may only book induction sessions', 403);

    const attendees = session.attendees ?? [];
    if (attendees.find((a: any) => a.userId === auth.userId)) return err('CONFLICT', 'Already booked', 409);

    const schedule = await ddb.send(new GetCommand({ TableName: 'rallyhub-schedules', Key: { id: session.scheduleId } }));
    const cap = session.reducedCapacity ?? schedule.Item?.capacityLimit ?? 999;

    if (attendees.length >= cap) return err('CAPACITY', 'Session is full — join the waitlist', 409);

    const bookedAt = new Date().toISOString();
    await ddb.send(new UpdateCommand({
      TableName: TABLES.SESSIONS,
      Key: { id: sessionId },
      UpdateExpression: 'SET attendees = list_append(if_not_exists(attendees, :empty), :attendee)',
      ExpressionAttributeValues: {
        ':attendee': [{ userId: auth.userId, bookedAt }],
        ':empty': [],
      },
    }));

    return ok({ booked: true, bookedAt });
  } catch (e: any) {
    if (e.message === 'UNAUTHORIZED') return err('UNAUTHORIZED', 'Authentication required', 401);
    return err('INTERNAL', e.message, 500);
  }
};
