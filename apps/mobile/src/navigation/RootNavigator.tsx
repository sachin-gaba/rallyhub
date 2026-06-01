import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useAuthStore } from '@/store/authStore';
import { AuthNavigator } from './AuthNavigator';
import { MemberNavigator } from './MemberNavigator';
import { OrganizerNavigator } from './OrganizerNavigator';

export type RootStackParamList = {
  Auth: undefined;
  Member: undefined;
  Organizer: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export function RootNavigator() {
  const { isAuthenticated, activeClubRole } = useAuthStore();

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {!isAuthenticated ? (
          <Stack.Screen name="Auth" component={AuthNavigator} />
        ) : activeClubRole?.startsWith('organizer') || activeClubRole === 'co_organizer' ? (
          <Stack.Screen name="Organizer" component={OrganizerNavigator} />
        ) : (
          <Stack.Screen name="Member" component={MemberNavigator} />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
