import type { APIGatewayProxyHandler } from 'aws-lambda';
import { GetCommand, PutCommand, UpdateCommand } from '@aws-sdk/lib-dynamodb';
import { v4 as uuid } from 'uuid';
import { ddb, TABLES } from '@/utils/dynamo';
import { ok, err } from '@/utils/response';
import { requireAuth } from '@/middleware/auth';

export const handler: APIGatewayProxyHandler = async (event) => {
  try {
    const auth = await requireAuth(event);
    const { clubId } = event.pathParameters ?? {};
    const body = JSON.parse(event.body ?? '{}');
    const { parqAnswers, medicalConditions, medications, emergencyContact, liabilityAccepted, dataProtectionConsented } = body;

    if (!liabilityAccepted || !dataProtectionConsented)
      return err('VALIDATION', 'Liability waiver and data consent are required');
    if (!emergencyContact?.fullName || !emergencyContact?.primaryPhone)
      return err('VALIDATION', 'Emergency contact name and phone are required');

    const declaration = {
      id: uuid(),
      userId: auth.userId,
      clubId,
      parqAnswers,
      medicalConditions,
      medications: medications ?? '',
      emergencyContact,
      liabilityAccepted,
      dataProtectionConsented,
      submittedAt: new Date().toISOString(),
      deviceInfo: event.headers['User-Agent'] ?? '',
      appVersion: event.headers['X-App-Version'] ?? '',
    };

    await Promise.all([
      ddb.send(new PutCommand({ TableName: TABLES.DECLARATIONS, Item: declaration })),
      ddb.send(new UpdateCommand({
        TableName: TABLES.MEMBERSHIPS,
        Key: { userId: auth.userId, clubId },
        UpdateExpression: 'SET healthDeclarationId = :id, healthDeclarationSubmitted = :t',
        ExpressionAttributeValues: { ':id': declaration.id, ':t': true },
      })),
    ]);

    return ok(declaration, 201);
  } catch (e: any) {
    if (e.message === 'UNAUTHORIZED') return err('UNAUTHORIZED', 'Authentication required', 401);
    return err('INTERNAL', e.message, 500);
  }
};
