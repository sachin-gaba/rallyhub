import type { APIGatewayProxyEvent } from 'aws-lambda';
import { CognitoJwtVerifier } from 'aws-jwt-verify';

const verifier = CognitoJwtVerifier.create({
  userPoolId: process.env.COGNITO_USER_POOL_ID!,
  tokenUse:   'id',
  clientId:   process.env.COGNITO_CLIENT_ID!,
});

export interface AuthContext {
  userId: string;
  email: string;
}

export async function requireAuth(event: APIGatewayProxyEvent): Promise<AuthContext> {
  const token = event.headers?.Authorization?.replace('Bearer ', '');
  if (!token) throw new Error('UNAUTHORIZED');
  const payload = await verifier.verify(token);
  return { userId: payload.sub, email: payload.email as string };
}
