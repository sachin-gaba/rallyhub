import React from 'react';
import { FlatList, Text, StyleSheet, Alert, ActivityIndicator, View } from 'react-native';
import { useUpcomingSessions, useBookSession, useCancelBooking } from '@/hooks/useSession';
import { useAuthStore } from '@/store/authStore';
import { api } from '@/services/api';
import { SessionCard } from '@/components/session/SessionCard';
import { useQueryClient } from '@tanstack/react-query';

export function SessionsScreen() {
  const activeClubId = useAuthStore((s) => s.activeClubId);
  const qc           = useQueryClient();
  const { data, isLoading } = useUpcomingSessions();
  const book    = useBookSession();
  const cancel  = useCancelBooking();

  function handleWaitlist(sessionId: string) {
    api.post(`/clubs/${activeClubId}/sessions/${sessionId}/waitlist`, {})
      .then(() => qc.invalidateQueries({ queryKey: ['sessions', activeClubId] }))
      .catch((e: any) => Alert.alert('Error', e.message));
  }

  if (isLoading) return <ActivityIndicator style={{ flex: 1 }} color="#2563EB" />;

  return (
    <FlatList
      style={styles.container}
      contentContainerStyle={{ paddingBottom: 32 }}
      data={data?.items ?? []}
      keyExtractor={(s) => s.id}
      ListHeaderComponent={<Text style={styles.header}>Sessions</Text>}
      ListEmptyComponent={<Text style={styles.empty}>No upcoming sessions</Text>}
      renderItem={({ item }) => (
        <SessionCard
          session={item}
          onBook={() => book.mutate(item.id, { onError: (e: any) => Alert.alert('Error', e.message) })}
          onCancel={() => Alert.alert('Cancel booking?', 'Credits will be refunded per policy.', [
            { text: 'Back', style: 'cancel' },
            { text: 'Cancel booking', style: 'destructive', onPress: () => cancel.mutate(item.id) },
          ])}
          onWaitlist={() => handleWaitlist(item.id)}
        />
      )}
    />
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  header:    { fontSize: 22, fontWeight: '700', color: '#111827', marginBottom: 16 },
  empty:     { color: '#9CA3AF', textAlign: 'center', marginTop: 40 },
});
