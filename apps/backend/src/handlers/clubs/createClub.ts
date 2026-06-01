import type { APIGatewayProxyHandler } from 'aws-lambda';
import { PutCommand } from '@aws-sdk/lib-dynamodb';
import { v4 as uuid } from 'uuid';
import { ddb, TABLES } from '@/utils/dynamo';
import { ok, err } from '@/utils/response';
import { requireAuth } from '@/middleware/auth';
import { DEFAULT_NEGATIVE_CREDIT_LIMIT, DEFAULT_WAITLIST_SEQUENTIAL_WINDOW_HOURS, DEFAULT_PANIC_WINDOW_HOURS_BEFORE_SESSION, DEFAULT_CANCELLATION_POLICY, DEFAULT_GUEST_SPONSOR_LIMIT_PER_MONTH, STARTER_FREE_PERIOD_DAYS } from '@rallyhub/shared';
import { addDays } from 'date-fns';

export const handler: APIGatewayProxyHandler = async (event) => {
  try {
    const auth = await requireAuth(event);
    const body = JSON.parse(event.body ?? '{}');
    const { name, sports, primaryVenue } = body;
    if (!name || !sports || !primaryVenue) return err('VALIDATION', 'name, sports, primaryVenue required');

    const clubId = uuid();
    const inviteCode = Math.random().toString(36).substring(2, 8).toUpperCase();
    const now = new Date().toISOString();

    const club = {
      id: clubId,
      name,
      sports,
      primaryVenue,
      inviteCode,
      plan: 'starter',
      planExpiresAt: addDays(new Date(), STARTER_FREE_PERIOD_DAYS).toISOString(),
      createdAt: now,
      settings: {
        negativeCreditLimit: DEFAULT_NEGATIVE_CREDIT_LIMIT,
        guestSponsorLimitPerMonth: DEFAULT_GUEST_SPONSOR_LIMIT_PER_MONTH,
        waitlistSequentialWindowHours: DEFAULT_WAITLIST_SEQUENTIAL_WINDOW_HOURS,
        panicWindowHoursBeforeSession: DEFAULT_PANIC_WINDOW_HOURS_BEFORE_SESSION,
        cancellationPolicy: { tiers: DEFAULT_CANCELLATION_POLICY },
      },
    };

    await ddb.send(new PutCommand({ TableName: TABLES.CLUBS, Item: club }));

    // Make creator the Primary Organizer
    const membership = {
      userId: auth.userId, clubId, role: 'organizer_primary',
      creditBalance: 0, paymentReference: `${inviteCode}-${auth.userId.substring(0, 4).toUpperCase()}`,
      joinedAt: now, inductionCompleted: true,
    };
    await ddb.send(new PutCommand({ TableName: TABLES.MEMBERSHIPS, Item: membership }));

    return ok(club, 201);
  } catch (e: any) {
    if (e.message === 'UNAUTHORIZED') return err('UNAUTHORIZED', 'Authentication required', 401);
    return err('INTERNAL', e.message, 500);
  }
};
