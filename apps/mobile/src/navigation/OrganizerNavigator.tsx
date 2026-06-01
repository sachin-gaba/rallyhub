import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { OrgDashboardScreen } from '@/screens/organizer/OrgDashboardScreen';
import { OrgSessionsScreen } from '@/screens/organizer/OrgSessionsScreen';
import { OrgMembersScreen } from '@/screens/organizer/OrgMembersScreen';
import { OrgFinanceScreen } from '@/screens/organizer/OrgFinanceScreen';
import { OrgSettingsScreen } from '@/screens/organizer/OrgSettingsScreen';

const Tab = createBottomTabNavigator();

export function OrganizerNavigator() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: '#2563EB',
        tabBarInactiveTintColor: '#6B7280',
      }}
    >
      <Tab.Screen name="Dashboard" component={OrgDashboardScreen} />
      <Tab.Screen name="Sessions" component={OrgSessionsScreen} />
      <Tab.Screen name="Members" component={OrgMembersScreen} />
      <Tab.Screen name="Finance" component={OrgFinanceScreen} />
      <Tab.Screen name="Settings" component={OrgSettingsScreen} />
    </Tab.Navigator>
  );
}
