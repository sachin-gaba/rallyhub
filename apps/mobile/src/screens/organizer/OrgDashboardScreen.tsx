import React from 'react';
import { View, Text, ScrollView, StyleSheet, TouchableOpacity, ActivityIndicator } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';

export function OrgDashboardScreen() {
  const { activeClubId } = useAuthStore();
  const { data: stats, isLoading } = useQuery({ queryKey: ['org-stats', activeClubId], queryFn: () => api.get<any>(`/clubs/${activeClubId}/stats`) });
  const { data: pending }           = useQuery({ queryKey: ['pending', activeClubId], queryFn: () => api.get<any>(`/clubs/${activeClubId}/pending-actions`) });

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ paddingBottom: 32 }}>
      <Text style={styles.header}>Organizer dashboard</Text>

      {/* Stats strip */}
      {isLoading ? <ActivityIndicator color="#2563EB" /> : (
        <View style={styles.statsRow}>
          {[
            { label: 'Members',         value: stats?.memberCount },
            { label: 'Active sessions', value: stats?.activeSessions },
            { label: 'Pending payments',value: stats?.pendingPayments },
          ].map((s) => (
            <View key={s.label} style={styles.statCard}>
              <Text style={styles.statValue}>{s.value ?? '—'}</Text>
              <Text style={styles.statLabel}>{s.label}</Text>
            </View>
          ))}
        </View>
      )}

      <Text style={styles.sectionTitle}>Pending actions</Text>
      {(pending?.items ?? []).map((a: any) => (
        <View key={a.id} style={styles.actionCard}>
          <View style={styles.actionDot} />
          <View style={{ flex: 1 }}>
            <Text style={styles.actionTitle}>{a.title}</Text>
            <Text style={styles.actionSub}>{a.subtitle}</Text>
          </View>
          <Text style={styles.actionCta}>Review →</Text>
        </View>
      ))}
      {(!pending?.items?.length) && <Text style={styles.empty}>All clear — no pending actions</Text>}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  header:       { fontSize: 22, fontWeight: '700', color: '#111827', marginBottom: 16 },
  statsRow:     { flexDirection: 'row', gap: 8, marginBottom: 20 },
  statCard:     { flex: 1, backgroundColor: '#2563EB', borderRadius: 12, padding: 14, alignItems: 'center' },
  statValue:    { color: '#fff', fontSize: 28, fontWeight: '800' },
  statLabel:    { color: '#BFDBFE', fontSize: 11, textAlign: 'center' },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#111827', marginBottom: 10 },
  actionCard:   { backgroundColor: '#fff', borderRadius: 12, padding: 16, marginBottom: 8, flexDirection: 'row', alignItems: 'center', gap: 12 },
  actionDot:    { width: 10, height: 10, borderRadius: 5, backgroundColor: '#F59E0B' },
  actionTitle:  { fontWeight: '600', color: '#111827' },
  actionSub:    { color: '#6B7280', fontSize: 13 },
  actionCta:    { color: '#2563EB', fontWeight: '600' },
  empty:        { color: '#9CA3AF', textAlign: 'center', marginTop: 20 },
});
