import React, { useState } from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity, Alert, ActivityIndicator } from 'react-native';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';

export function OrgSessionsScreen() {
  const { activeClubId } = useAuthStore();
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ['org-sessions', activeClubId], queryFn: () => api.get<any>(`/clubs/${activeClubId}/sessions`) });

  const markComplete = useMutation({
    mutationFn: (id: string) => api.patch(`/clubs/${activeClubId}/sessions/${id}/complete`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['org-sessions'] }),
  });

  const cancelSession = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      api.patch(`/clubs/${activeClubId}/sessions/${id}/cancel`, { reason }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['org-sessions'] }),
    onError: (e: any) => Alert.alert('Error', e.message),
  });

  return (
    <View style={styles.container}>
      <Text style={styles.header}>Sessions</Text>
      {isLoading ? <ActivityIndicator color="#2563EB" /> : (
        <FlatList
          data={data?.items ?? []}
          keyExtractor={(s) => s.id}
          renderItem={({ item: s }) => (
            <View style={styles.card}>
              <View style={{ flex: 1 }}>
                <Text style={styles.date}>{new Date(s.date).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })}</Text>
                <Text style={styles.name}>{s.scheduleName}</Text>
                <Text style={styles.meta}>{s.attendeeCount}/{s.capacity} · {s.waitlistCount} waiting</Text>
              </View>
              {s.status === 'scheduled' && (
                <View style={{ gap: 6 }}>
                  <TouchableOpacity style={styles.completeBtn} onPress={() => markComplete.mutate(s.id)}>
                    <Text style={styles.completeBtnText}>Complete</Text>
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.cancelBtn}
                    onPress={() => Alert.prompt('Cancel session', 'Reason (optional)', (reason) => cancelSession.mutate({ id: s.id, reason: reason ?? '' }))}>
                    <Text style={styles.cancelBtnText}>Cancel</Text>
                  </TouchableOpacity>
                </View>
              )}
              {s.status !== 'scheduled' && (
                <View style={[styles.statusPill, s.status === 'cancelled' ? styles.statusCancelled : styles.statusDone]}>
                  <Text style={styles.statusText}>{s.status}</Text>
                </View>
              )}
            </View>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container:        { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  header:           { fontSize: 22, fontWeight: '700', color: '#111827', marginBottom: 16 },
  card:             { backgroundColor: '#fff', borderRadius: 12, padding: 16, marginBottom: 8, flexDirection: 'row', alignItems: 'center', borderLeftWidth: 4, borderLeftColor: '#2563EB' },
  date:             { color: '#6B7280', fontSize: 13 },
  name:             { fontWeight: '600', color: '#111827', fontSize: 16 },
  meta:             { color: '#9CA3AF', fontSize: 12 },
  completeBtn:      { backgroundColor: '#DCFCE7', borderRadius: 6, paddingHorizontal: 10, paddingVertical: 5 },
  completeBtnText:  { color: '#16A34A', fontWeight: '700', fontSize: 12 },
  cancelBtn:        { backgroundColor: '#FEE2E2', borderRadius: 6, paddingHorizontal: 10, paddingVertical: 5 },
  cancelBtnText:    { color: '#DC2626', fontWeight: '700', fontSize: 12 },
  statusPill:       { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 20 },
  statusCancelled:  { backgroundColor: '#FEE2E2' },
  statusDone:       { backgroundColor: '#DCFCE7' },
  statusText:       { fontSize: 12, fontWeight: '600', color: '#374151', textTransform: 'capitalize' },
});
