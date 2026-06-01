import React from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity, ActivityIndicator, Alert } from 'react-native';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';

export function OrgFinanceScreen() {
  const { activeClubId } = useAuthStore();
  const qc = useQueryClient();
  const { data: payments, isLoading } = useQuery({ queryKey: ['payments', activeClubId], queryFn: () => api.get<any>(`/clubs/${activeClubId}/payments/pending`) });

  const verify = useMutation({
    mutationFn: ({ paymentId, credits }: { paymentId: string; credits: number }) =>
      api.post(`/clubs/${activeClubId}/payments/${paymentId}/verify`, { creditsToAdd: credits }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['payments', activeClubId] }),
    onError: (e: any) => Alert.alert('Error', e.message),
  });

  return (
    <View style={styles.container}>
      <Text style={styles.header}>Finance</Text>
      <TouchableOpacity style={styles.exportBtn}><Text style={styles.exportText}>Export CSV</Text></TouchableOpacity>

      <Text style={styles.sectionTitle}>Pending payments to verify</Text>
      {isLoading ? <ActivityIndicator color="#2563EB" /> : (
        <FlatList
          data={payments?.items ?? []}
          keyExtractor={(p) => p.id}
          ListEmptyComponent={<Text style={styles.empty}>No pending payments</Text>}
          renderItem={({ item: p }) => (
            <View style={styles.card}>
              <View style={{ flex: 1 }}>
                <Text style={styles.memberName}>{p.memberName}</Text>
                <Text style={styles.ref}>Ref: {p.reference}</Text>
                <Text style={styles.amount}>£{p.amount} claimed</Text>
              </View>
              <TouchableOpacity style={styles.verifyBtn}
                onPress={() => Alert.alert('Verify payment', `Add how many credits to ${p.memberName}?`, [
                  { text: 'Cancel', style: 'cancel' },
                  { text: 'Confirm', onPress: () => verify.mutate({ paymentId: p.id, credits: Math.round(p.amount / p.creditPrice) }) },
                ])}>
                <Text style={styles.verifyText}>Verify</Text>
              </TouchableOpacity>
            </View>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  header:       { fontSize: 22, fontWeight: '700', color: '#111827', marginBottom: 8 },
  exportBtn:    { alignSelf: 'flex-end', marginBottom: 16 },
  exportText:   { color: '#2563EB', fontWeight: '600' },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#111827', marginBottom: 10 },
  card:         { backgroundColor: '#fff', borderRadius: 12, padding: 16, marginBottom: 8, flexDirection: 'row', alignItems: 'center' },
  memberName:   { fontWeight: '600', color: '#111827' },
  ref:          { color: '#6B7280', fontSize: 12 },
  amount:       { color: '#374151', fontWeight: '600', marginTop: 2 },
  verifyBtn:    { backgroundColor: '#DCFCE7', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 8 },
  verifyText:   { color: '#16A34A', fontWeight: '700' },
  empty:        { color: '#9CA3AF', textAlign: 'center', marginTop: 20 },
});
