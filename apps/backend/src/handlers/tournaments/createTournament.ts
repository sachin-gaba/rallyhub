import type { APIGatewayProxyHandler } from 'aws-lambda';
import { GetCommand, PutCommand } from '@aws-sdk/lib-dynamodb';
import { v4 as uuid } from 'uuid';
import { ddb, TABLES } from '@/utils/dynamo';
import { ok, err } from '@/utils/response';
import { requireAuth } from '@/middleware/auth';
import type { TiebreakerRule } from '@rallyhub/shared';

const DEFAULT_TIEBREAKERS: TiebreakerRule[] = ['wins', 'head_to_head', 'points_difference', 'total_points', 'coin_flip'];

function buildSkillMatchedGroups(participants: any[], groupSize: number) {
  const sorted = [...participants].sort((a, b) => a.skillTier.localeCompare(b.skillTier));
  const groups: any[][] = [];
  for (let i = 0; i < sorted.length; i += groupSize) groups.push(sorted.slice(i, i + groupSize));
  return groups;
}

export const handler: APIGatewayProxyHandler = async (event) => {
  try {
    const auth = await requireAuth(event);
    const { clubId } = event.pathParameters ?? {};
    const body = JSON.parse(event.body ?? '{}');
    const { name, format, drawType, sport, participantIds, groupSize = 4 } = body;

    if (!name || !format || !drawType || !sport || !participantIds?.length)
      return err('VALIDATION', 'name, format, drawType, sport, participantIds required');

    // Fetch memberships to get skill tiers
    const memberData = await Promise.all(participantIds.map((uid: string) =>
      ddb.send(new GetCommand({ TableName: TABLES.MEMBERSHIPS, Key: { userId: uid, clubId } }))));
    const participants = memberData.map((r) => ({ userId: r.Item?.userId, skillTier: r.Item?.skillLevel ?? 'intermediate' }));

    const groups = drawType === 'skill_matched'
      ? buildSkillMatchedGroups(participants, groupSize)
      : [participants]; // random / mixed handled similarly

    const tournamentGroups = groups.map((g, i) => ({ id: uuid(), name: `Group ${String.fromCharCode(65 + i)}`, participantIds: g.map((p) => p.userId) }));

    const tournament = {
      id: uuid(),
      clubId, name, format, drawType, sport,
      participants,
      groups: tournamentGroups,
      matches: [],
      tiebreakerOrder: DEFAULT_TIEBREAKERS,
      createdAt: new Date().toISOString(),
    };

    await ddb.send(new PutCommand({ TableName: TABLES.TOURNAMENTS, Item: tournament }));
    return ok(tournament, 201);
  } catch (e: any) {
    if (e.message === 'UNAUTHORIZED') return err('UNAUTHORIZED', 'Authentication required', 401);
    return err('INTERNAL', e.message, 500);
  }
};
