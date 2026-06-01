import {
  CognitoUserPool,
  CognitoUser,
  AuthenticationDetails,
  CognitoUserAttribute,
} from 'amazon-cognito-identity-js';

const poolData = {
  UserPoolId: process.env.EXPO_PUBLIC_COGNITO_USER_POOL_ID ?? '',
  ClientId:   process.env.EXPO_PUBLIC_COGNITO_CLIENT_ID ?? '',
};

const userPool = new CognitoUserPool(poolData);

export async function signUp(email: string, password: string, displayName: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const attributes = [
      new CognitoUserAttribute({ Name: 'email', Value: email }),
      new CognitoUserAttribute({ Name: 'name',  Value: displayName }),
    ];
    userPool.signUp(email, password, attributes, [], (err) => {
      if (err) return reject(err);
      resolve();
    });
  });
}

export async function confirmSignUp(email: string, code: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const user = new CognitoUser({ Username: email, Pool: userPool });
    user.confirmRegistration(code, true, (err) => {
      if (err) return reject(err);
      resolve();
    });
  });
}

export async function signIn(email: string, password: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const authDetails = new AuthenticationDetails({ Username: email, Password: password });
    const user = new CognitoUser({ Username: email, Pool: userPool });
    user.authenticateUser(authDetails, {
      onSuccess: (session) => resolve(session.getIdToken().getJwtToken()),
      onFailure: (err) => reject(err),
    });
  });
}

export async function signOut(): Promise<void> {
  const user = userPool.getCurrentUser();
  user?.signOut();
}

export async function getSession(): Promise<string | null> {
  return new Promise((resolve) => {
    const user = userPool.getCurrentUser();
    if (!user) return resolve(null);
    user.getSession((err: Error | null, session: any) => {
      if (err || !session?.isValid()) return resolve(null);
      resolve(session.getIdToken().getJwtToken());
    });
  });
}
