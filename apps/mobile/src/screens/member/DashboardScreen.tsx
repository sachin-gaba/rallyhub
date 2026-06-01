import React from 'react';
import { View, Text, ScrollView, StyleSheet, TouchableOpacity } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';

export function DashboardScreen() {
  const { activeClubId, displayName } = useAuthStore();
  const { data: membership } = useQuery({ queryKey: ['membership', activeClubId], queryFn: () => api.get<any>(`/clubs/${activeClubId}/membership/me`) });
  const { data: sessions }   = useQuery({ queryKey: ['sessions', activeClubId],   queryFn: () => api.get<any>(`/clubs/${activeClubId}/sessions?upcoming=true`) });
  const { data: announcements } = useQuery({ queryKey: ['announcements', activeClubId], queryFn: () => api.get<any>(`/clubs/${activeClubId}/announcements`) });

  const credits = membership?.creditBalance ?? '—';

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ paddingBottom: 32 }}>
      <Text style={styles.greeting}>Hey, {displayName} 👋</Text>

      {/* Credit card */}
      <View style={styles.creditCard}>
        <Text style={styles.creditLabel}>Credit balance</Text>
        <Text style={styles.creditValue}>{credits} credits</Text>
        <TouchableOpacity style={styles.topUpBtn}><Text style={styles.topUpText}>Top up →</Text></TouchableOpacity>
      </View>

      {/* Upcoming sessions */}
      <Text style={styles.sectionTitle}>Upcoming sessions</Text>
      {(sessions?.items ?? []).slice(0, 3).map((s: any) => (
        <View key={s.id} style={styles.sessionCard}>
          <Text style={styles.sessionDate}>{new Date(s.date).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })}</Text>
          <Text style={styles.sessionName}>{s.scheduleName}</Text>
          <View style={[styles.pill, s.booked ? styles.pillBooked : styles.pillOpen]}>
            <Text style={styles.pillText}>{s.booked ? 'Booked' : 'Open'}</Text>
          </View>
        </View>
      ))}

      {/* Announcements */}
      <Text style={styles.sectionTitle}>Announcements</Text>
      {(announcements?.items ?? []).map((a: any) => (
        <View key={a.id} style={styles.announcementCard}>
          <Text style={styles.announcementBody}>{a.body}</Text>
          <Text style={styles.announcementDate}>{new Date(a.createdAt).toLocaleDateString('en-GB')}</Text>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:        { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  greeting:         { fontSize: 22, fontWeight: '700', color: '#111827', marginBottom: 16 },
  creditCard:       { backgroundColor: '#2563EB', borderRadius: 16, padding: 24, marginBottom: 20 },
  creditLabel:      { color: '#BFDBFE', fontSize: 14 },
  creditValue:      { color: '#fff', fontSize: 36, fontWeight: '800', marginVertical: 4 },
  topUpBtn:         { alignSelf: 'flex-end' },
  topUpText:        { color: '#BFDBFE', fontWeight: '600' },
  sectionTitle:     { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 10, marginTop: 4 },
  sessionCard:      { backgroundColor: '#fff', borderRadius: 12, padding: 16, marginBottom: 8, flexDirection: 'row', alignItems: 'center', borderLeftWidth: 4, borderLeftColor: '#2563EB' },
  sessionDate:      { width: 72, color: '#6B7280', fontSize: 13 },
  sessionName:      { flex: 1, fontWeight: '600', color: '#111827' },
  pill:             { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 20 },
  pillBooked:       { backgroundColor: '#DCFCE7' },
  pillOpen:         { backgroundColor: '#FEF3C7' },
  pillText:         { fontSize: 12, fontWeight: '600', color: '#374151' },
  announcementCard: { backgroundColor: '#fff', borderRadius: 12, padding: 16, marginBottom: 8 },
  announcementBody: { color: '#374151' },
  announcementDate: { color: '#9CA3AF', fontSize: 12, marginTop: 4 },
});
