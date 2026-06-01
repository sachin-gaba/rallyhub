import React from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity, Alert, ActivityIndicator } from 'react-native';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';

export function SessionsScreen() {
  const { activeClubId } = useAuthStore();
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ['sessions', activeClubId], queryFn: () => api.get<any>(`/clubs/${activeClubId}/sessions?upcoming=true`) });

  const book = useMutation({
    mutationFn: (sessionId: string) => api.post(`/clubs/${activeClubId}/sessions/${sessionId}/book`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sessions', activeClubId] }),
    onError: (e: any) => Alert.alert('Booking failed', e.message),
  });

  const cancel = useMutation({
    mutationFn: (sessionId: string) => api.delete(`/clubs/${activeClubId}/sessions/${sessionId}/book`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sessions', activeClubId] }),
    onError: (e: any) => Alert.alert('Cancel failed', e.message),
  });

  if (isLoading) return <ActivityIndicator style={{ flex: 1 }} color="#2563EB" />;

  return (
    <FlatList
      style={styles.container}
      contentContainerStyle={{ paddingBottom: 32 }}
      data={data?.items ?? []}
      keyExtractor={(s) => s.id}
      ListHeaderComponent={<Text style={styles.header}>Sessions</Text>}
      renderItem={({ item: s }) => (
        <View style={styles.card}>
          <View style={{ flex: 1 }}>
            <Text style={styles.date}>{new Date(s.date).toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'short' })}</Text>
            <Text style={styles.name}>{s.scheduleName}</Text>
            <Text style={styles.meta}>{s.venue} · {s.attendeeCount}/{s.capacity} players · {s.priceCredits} credit{s.priceCredits !== 1 ? 's' : ''}</Text>
          </View>
          {s.booked ? (
            <TouchableOpacity style={styles.cancelBtn} onPress={() => cancel.mutate(s.id)}>
              <Text style={styles.cancelText}>Cancel</Text>
            </TouchableOpacity>
          ) : s.attendeeCount >= s.capacity ? (
            <TouchableOpacity style={styles.waitlistBtn} onPress={() => api.post(`/clubs/${activeClubId}/sessions/${s.id}/waitlist`, {})}>
              <Text style={styles.waitlistText}>Waitlist</Text>
            </TouchableOpacity>
          ) : (
            <TouchableOpacity style={styles.bookBtn} onPress={() => book.mutate(s.id)}>
              <Text style={styles.bookText}>Book</Text>
            </TouchableOpacity>
          )}
        </View>
      )}
    />
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  header:       { fontSize: 22, fontWeight: '700', color: '#111827', marginBottom: 16 },
  card:         { backgroundColor: '#fff', borderRadius: 12, padding: 16, marginBottom: 10, flexDirection: 'row', alignItems: 'center', borderLeftWidth: 4, borderLeftColor: '#2563EB' },
  date:         { color: '#6B7280', fontSize: 13 },
  name:         { fontWeight: '600', color: '#111827', fontSize: 16 },
  meta:         { color: '#9CA3AF', fontSize: 12, marginTop: 2 },
  bookBtn:      { backgroundColor: '#2563EB', borderRadius: 8, paddingHorizontal: 16, paddingVertical: 8 },
  bookText:     { color: '#fff', fontWeight: '700' },
  cancelBtn:    { backgroundColor: '#FEE2E2', borderRadius: 8, paddingHorizontal: 16, paddingVertical: 8 },
  cancelText:   { color: '#DC2626', fontWeight: '700' },
  waitlistBtn:  { backgroundColor: '#FEF3C7', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 8 },
  waitlistText: { color: '#92400E', fontWeight: '700', fontSize: 13 },
});
