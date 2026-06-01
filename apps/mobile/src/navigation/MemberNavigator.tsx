import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { DashboardScreen } from '@/screens/member/DashboardScreen';
import { SessionsScreen } from '@/screens/member/SessionsScreen';
import { WalletScreen } from '@/screens/member/WalletScreen';
import { TournamentScreen } from '@/screens/member/TournamentScreen';
import { ProfileScreen } from '@/screens/member/ProfileScreen';
import { WaitlistScreen } from '@/screens/member/WaitlistScreen';
import { HealthDeclarationScreen } from '@/screens/member/HealthDeclarationScreen';

const Tab = createBottomTabNavigator();

export function MemberNavigator() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: '#2563EB',
        tabBarInactiveTintColor: '#6B7280',
      }}
    >
      <Tab.Screen name="Dashboard" component={DashboardScreen} />
      <Tab.Screen name="Sessions" component={SessionsScreen} />
      <Tab.Screen name="Wallet" component={WalletScreen} />
      <Tab.Screen name="Tournament" component={TournamentScreen} />
      <Tab.Screen name="Profile" component={ProfileScreen} />
    </Tab.Navigator>
  );
}
