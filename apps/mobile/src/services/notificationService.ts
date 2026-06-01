import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';
import { Platform } from 'react-native';
import { api } from './api';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge:  true,
  }),
});

/**
 * Request push permission and register the device token with the backend.
 * Called after the user signs in and a clubId is known.
 */
export async function registerForPushNotifications(): Promise<string | null> {
  if (!Device.isDevice) {
    console.warn('Push notifications require a physical device');
    return null;
  }

  const { status: existingStatus } = await Notifications.getPermissionsAsync();
  let finalStatus = existingStatus;

  if (existingStatus !== 'granted') {
    const { status } = await Notifications.requestPermissionsAsync();
    finalStatus = status;
  }

  if (finalStatus !== 'granted') {
    console.warn('Push notification permission denied');
    return null;
  }

  // Get the Expo push token (used for SNS endpoint registration on backend)
  const token = (await Notifications.getExpoPushTokenAsync()).data;

  // Android channel
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name:       'RallyHub',
      importance: Notifications.AndroidImportance.MAX,
      vibrationPattern: [0, 250, 250, 250],
      lightColor: '#2563EB',
    });
  }

  // Register token with backend
  try {
    await api.patch('/me/push-token', { pushToken: token });
  } catch (e) {
    console.warn('Failed to register push token with backend:', e);
  }

  return token;
}

/**
 * Set up foreground notification listeners.
 * Returns cleanup function — call in useEffect cleanup.
 */
export function setupNotificationListeners(
  onNotification: (notification: Notifications.Notification) => void,
  onResponse: (response: Notifications.NotificationResponse) => void
): () => void {
  const receivedSub  = Notifications.addNotificationReceivedListener(onNotification);
  const responseSub  = Notifications.addNotificationResponseReceivedListener(onResponse);
  return () => {
    receivedSub.remove();
    responseSub.remove();
  };
}

/** Schedule a local session reminder (fallback when push is unavailable). */
export async function scheduleSessionReminder(
  sessionId: string,
  sessionDate: Date,
  hoursAhead: number
): Promise<string> {
  const triggerDate = new Date(sessionDate.getTime() - hoursAhead * 60 * 60 * 1000);
  return Notifications.scheduleNotificationAsync({
    identifier: `session-reminder-${sessionId}-${hoursAhead}h`,
    content: {
      title: 'Session reminder',
      body:  `Your session starts in ${hoursAhead} hour${hoursAhead !== 1 ? 's' : ''}`,
      data:  { sessionId },
    },
    trigger: { date: triggerDate },
  });
}

export async function cancelSessionReminder(sessionId: string, hoursAhead: number): Promise<void> {
  await Notifications.cancelScheduledNotificationAsync(
    `session-reminder-${sessionId}-${hoursAhead}h`
  );
}
