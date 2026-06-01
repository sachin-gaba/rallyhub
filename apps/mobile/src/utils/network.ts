import NetInfo from '@react-native-community/netinfo';

/** Returns true if device has network connectivity. */
export async function isOnline(): Promise<boolean> {
  const state = await NetInfo.fetch();
  return !!(state.isConnected && state.isInternetReachable);
}

/** Shows an offline message to the user. */
export const OFFLINE_MESSAGE =
  "This action needs an internet connection. We'll remind you when you're back online.";
