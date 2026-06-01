import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';

export function WaitlistScreen({ sessionId }: { sessionId: string }) {
  const { activeClubId } = useAuthStore();
  const qc = useQueryClient();
  const { data } = useQuery({ queryKey: ['waitlist', activeClubId, sessionId], queryFn: () => api.get<any>(`/clubs/${activeClubId}/sessions/${sessionId}/waitlist/me`) });

  const leave = useMutation({
    mutationFn: () => api.delete(`/clubs/${activeClubId}/sessions/${sessionId}/waitlist`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['waitlist'] }),
  });

  const confirm = useMutation({
    mutationFn: () => api.post(`/clubs/${activeClubId}/sessions/${sessionId}/waitlist/confirm`, {}),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['sessions'] }); Alert.alert('You\'re in!', 'Your spot is confirmed.'); },
  });

  return (
    <View style={styles.container}>
      <View style={styles.positionCard}>
        <Text style={styles.posLabel}>Your position</Text>
        <Text style={styles.posNumber}>#{data?.position ?? '—'}</Text>
        <Text style={styles.posInfo}>You'll be notified when a slot opens</Text>
      </View>

      <View style={styles.stepCard}><Text style={styles.stepNum}>1</Text><Text style={styles.stepText}>A slot opens — you're notified exclusively for {data?.windowHours ?? 2} hours</Text></View>
      <View style={styles.stepCard}><Text style={styles.stepNum}>2</Text><Text style={styles.stepText}>If you don't respond, the slot passes to the next person</Text></View>
      <View style={styles.stepCard}><Text style={styles.stepNum}>3</Text><Text style={styles.stepText}>Within 24 hours of the session, all waitlisted members are notified simultaneously (first come, first served)</Text></View>

      {data?.slotAvailable && (
        <TouchableOpacity style={styles.confirmBtn} onPress={() => confirm.mutate()}>
          <Text style={styles.confirmText}>Confirm my spot now!</Text>
        </TouchableOpacity>
      )}
      <TouchableOpacity style={styles.leaveBtn} onPress={() => leave.mutate()}>
        <Text style={styles.leaveText}>Leave waitlist</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  positionCard: { backgroundColor: '#2563EB', borderRadius: 16, padding: 32, alignItems: 'center', marginBottom: 20 },
  posLabel:     { color: '#BFDBFE' },
  posNumber:    { color: '#fff', fontSize: 64, fontWeight: '800' },
  posInfo:      { color: '#BFDBFE', marginTop: 4 },
  stepCard:     { backgroundColor: '#fff', borderRadius: 12, padding: 16, marginBottom: 8, flexDirection: 'row', gap: 12, alignItems: 'flex-start' },
  stepNum:      { width: 28, height: 28, borderRadius: 14, backgroundColor: '#EFF6FF', color: '#2563EB', fontWeight: '700', textAlign: 'center', lineHeight: 28 },
  stepText:     { flex: 1, color: '#374151' },
  confirmBtn:   { backgroundColor: '#16A34A', borderRadius: 10, padding: 16, alignItems: 'center', marginTop: 16 },
  confirmText:  { color: '#fff', fontWeight: '700', fontSize: 16 },
  leaveBtn:     { borderWidth: 1, borderColor: '#DC2626', borderRadius: 10, padding: 14, alignItems: 'center', marginTop: 10 },
  leaveText:    { color: '#DC2626', fontWeight: '600' },
});
