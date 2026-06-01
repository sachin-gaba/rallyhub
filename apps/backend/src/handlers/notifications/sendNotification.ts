import { SNSClient, PublishCommand } from '@aws-sdk/client-sns';
import { SESClient, SendEmailCommand } from '@aws-sdk/client-ses';
import type { NotificationEvent } from '@rallyhub/shared';

const sns = new SNSClient({ region: process.env.AWS_REGION ?? 'eu-west-1' });
const ses = new SESClient({ region: process.env.AWS_REGION ?? 'eu-west-1' });

export interface NotificationPayload {
  userId: string;
  deviceToken?: string;
  email?: string;
  event: NotificationEvent;
  title: string;
  body: string;
  data?: Record<string, string>;
}

const TIME_CRITICAL: NotificationEvent[] = ['waitlist_slot_sequential', 'waitlist_panic_window', 'session_cancelled'];

export async function sendNotification(payload: NotificationPayload): Promise<void> {
  const { deviceToken, email, event, title, body, data } = payload;

  let pushSent = false;
  if (deviceToken) {
    try {
      await sns.send(new PublishCommand({
        TargetArn: deviceToken,
        Message: JSON.stringify({ default: body, APNS: JSON.stringify({ aps: { alert: { title, body }, sound: 'default' }, data }), GCM: JSON.stringify({ notification: { title, body }, data }) }),
        MessageStructure: 'json',
      }));
      pushSent = true;
    } catch {
      // push failed — fall through to email fallback
    }
  }

  // Email fallback for time-critical events
  if (!pushSent && email && TIME_CRITICAL.includes(event)) {
    await ses.send(new SendEmailCommand({
      Source: 'noreply@rallyhub.app',
      Destination: { ToAddresses: [email] },
      Message: {
        Subject: { Data: title },
        Body: { Text: { Data: body } },
      },
    }));
  }
}
