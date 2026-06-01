import type { APIGatewayProxyResult } from 'aws-lambda';

const CORS = {
  'Access-Control-Allow-Origin':  '*',
  'Access-Control-Allow-Methods': 'GET,POST,PUT,PATCH,DELETE,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type,Authorization',
};

export function ok<T>(data: T, status = 200): APIGatewayProxyResult {
  return { statusCode: status, headers: CORS, body: JSON.stringify({ data }) };
}

export function err(code: string, message: string, status = 400): APIGatewayProxyResult {
  return { statusCode: status, headers: CORS, body: JSON.stringify({ error: { code, message } }) };
}

export function noContent(): APIGatewayProxyResult {
  return { statusCode: 204, headers: CORS, body: '' };
}
