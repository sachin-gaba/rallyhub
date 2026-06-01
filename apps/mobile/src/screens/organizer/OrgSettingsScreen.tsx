import React from 'react';
import { View, Text, ScrollView, StyleSheet, TouchableOpacity, Switch } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';

export function OrgSettingsScreen() {
  const { activeClubId } = useAuthStore();
  const { data: club } = useQuery({ queryKey: ['club', activeClubId], queryFn: () => api.get<any>(`/clubs/${activeClubId}`) });

  const rows = [
    { label: 'Club details', sub: club?.name },
    { label: 'Bank transfer details' },
    { label: 'Session schedules' },
    { label: 'Cancellation policy', sub: 'Configure refund tiers' },
    { label: 'Credit & payment settings' },
    { label: 'Waitlist settings', sub: `${club?.settings?.waitlistSequentialWindowHours ?? 2}h sequential window` },
    { label: 'Guest player rules' },
    { label: 'Notification preferences' },
    { label: 'Co-organizer management' },
    { label: 'Invite link & QR code' },
    { label: 'Export member data (CSV)' },
    { label: 'Club subscription & billing' },
    { label: 'Transfer ownership', danger: true },
    { label: 'Delete club', danger: true },
  ];

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ paddingBottom: 40 }}>
      <Text style={styles.header}>Settings</Text>
      <View style={styles.section}>
        {rows.map((r, i) => (
          <TouchableOpacity key={i} style={[styles.row, i < rows.length - 1 && styles.rowBorder]}>
            <View style={{ flex: 1 }}>
              <Text style={[styles.rowLabel, r.danger && styles.danger]}>{r.label}</Text>
              {r.sub && <Text style={styles.rowSub}>{r.sub}</Text>}
            </View>
            <Text style={styles.chevron}>›</Text>
          </TouchableOpacity>
        ))}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  header:    { fontSize: 22, fontWeight: '700', color: '#111827', marginBottom: 16 },
  section:   { backgroundColor: '#fff', borderRadius: 12 },
  row:       { flexDirection: 'row', alignItems: 'center', padding: 16 },
  rowBorder: { borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
  rowLabel:  { color: '#111827', fontWeight: '500' },
  rowSub:    { color: '#9CA3AF', fontSize: 12, marginTop: 2 },
  danger:    { color: '#DC2626' },
  chevron:   { color: '#9CA3AF', fontSize: 18 },
});
