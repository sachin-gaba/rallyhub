import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import { useAuthStore } from '@/store/authStore';
import { signOut } from '@/services/authService';
import { SKILL_TIER_LABELS } from '@rallyhub/shared';

export function ProfileScreen() {
  const { displayName, email, clearAuth, activeClubRole } = useAuthStore();

  async function handleSignOut() {
    await signOut();
    clearAuth();
  }

  return (
    <View style={styles.container}>
      <View style={styles.avatar}><Text style={styles.avatarText}>{displayName?.charAt(0).toUpperCase()}</Text></View>
      <Text style={styles.name}>{displayName}</Text>
      <Text style={styles.email}>{email}</Text>
      <View style={styles.badge}><Text style={styles.badgeText}>{activeClubRole?.replace(/_/g, ' ')}</Text></View>

      <View style={styles.section}>
        <TouchableOpacity style={styles.row}><Text style={styles.rowText}>Notification preferences</Text><Text>›</Text></TouchableOpacity>
        <TouchableOpacity style={styles.row}><Text style={styles.rowText}>Data & privacy</Text><Text>›</Text></TouchableOpacity>
        <TouchableOpacity style={styles.row}><Text style={styles.rowText}>Calendar sync (iCal)</Text><Text>›</Text></TouchableOpacity>
        <TouchableOpacity style={styles.row}><Text style={styles.rowText}>Export my data</Text><Text>›</Text></TouchableOpacity>
      </View>

      <TouchableOpacity style={styles.signOutBtn} onPress={() => Alert.alert('Sign out', 'Are you sure?', [{ text: 'Cancel' }, { text: 'Sign out', onPress: handleSignOut }])}>
        <Text style={styles.signOutText}>Sign out</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container:   { flex: 1, backgroundColor: '#F9FAFB', padding: 24, alignItems: 'center' },
  avatar:      { width: 80, height: 80, borderRadius: 40, backgroundColor: '#2563EB', justifyContent: 'center', alignItems: 'center', marginBottom: 12 },
  avatarText:  { color: '#fff', fontSize: 32, fontWeight: '700' },
  name:        { fontSize: 22, fontWeight: '700', color: '#111827' },
  email:       { color: '#6B7280', marginBottom: 8 },
  badge:       { backgroundColor: '#EFF6FF', paddingHorizontal: 12, paddingVertical: 4, borderRadius: 20, marginBottom: 24 },
  badgeText:   { color: '#2563EB', fontWeight: '600', textTransform: 'capitalize' },
  section:     { width: '100%', backgroundColor: '#fff', borderRadius: 12, marginBottom: 16 },
  row:         { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 16, borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
  rowText:     { color: '#374151' },
  signOutBtn:  { borderWidth: 1, borderColor: '#DC2626', borderRadius: 10, paddingHorizontal: 32, paddingVertical: 12 },
  signOutText: { color: '#DC2626', fontWeight: '600' },
});
