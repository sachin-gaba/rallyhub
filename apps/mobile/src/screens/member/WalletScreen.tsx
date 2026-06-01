import React, { useState } from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity, TextInput, Alert, ActivityIndicator } from 'react-native';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';

const AMOUNTS = [5, 10, 20, 50];

export function WalletScreen() {
  const { activeClubId } = useAuthStore();
  const qc = useQueryClient();
  const [amount, setAmount] = useState('');
  const { data: membership }  = useQuery({ queryKey: ['membership', activeClubId], queryFn: () => api.get<any>(`/clubs/${activeClubId}/membership/me`) });
  const { data: ledger, isLoading } = useQuery({ queryKey: ['ledger', activeClubId], queryFn: () => api.get<any>(`/clubs/${activeClubId}/ledger/me`) });

  const markPaid = useMutation({
    mutationFn: (amt: number) => api.post(`/clubs/${activeClubId}/payments/mark-paid`, { amount: amt }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['membership', activeClubId] }); Alert.alert('Payment submitted', 'The organizer will verify your transfer.'); },
    onError: (e: any) => Alert.alert('Error', e.message),
  });

  return (
    <View style={styles.container}>
      <View style={styles.creditCard}>
        <Text style={styles.label}>Credit balance</Text>
        <Text style={styles.balance}>{membership?.creditBalance ?? '—'}</Text>
        <Text style={styles.ref}>Your reference: <Text style={styles.refBold}>{membership?.paymentReference ?? '—'}</Text></Text>
      </View>

      <Text style={styles.sectionTitle}>Mark a bank transfer as paid</Text>
      <View style={styles.row}>
        {AMOUNTS.map((a) => (
          <TouchableOpacity key={a} style={[styles.amtBtn, amount === String(a) && styles.amtBtnActive]} onPress={() => setAmount(String(a))}>
            <Text style={[styles.amtText, amount === String(a) && styles.amtTextActive]}>£{a}</Text>
          </TouchableOpacity>
        ))}
      </View>
      <TextInput style={styles.input} placeholder="Or enter amount (£)" value={amount} onChangeText={setAmount} keyboardType="decimal-pad" />
      <TouchableOpacity style={styles.btn} onPress={() => markPaid.mutate(parseFloat(amount))}>
        <Text style={styles.btnText}>Mark as paid</Text>
      </TouchableOpacity>

      <Text style={styles.sectionTitle}>Transaction history</Text>
      {isLoading ? <ActivityIndicator color="#2563EB" /> : (
        <FlatList
          data={ledger?.items ?? []}
          keyExtractor={(e) => e.id}
          renderItem={({ item: e }) => (
            <View style={styles.txRow}>
              <View style={{ flex: 1 }}>
                <Text style={styles.txType}>{e.type.replace(/_/g, ' ')}</Text>
                <Text style={styles.txDate}>{new Date(e.createdAt).toLocaleDateString('en-GB')}</Text>
              </View>
              <Text style={[styles.txAmount, e.amount > 0 ? styles.positive : styles.negative]}>
                {e.amount > 0 ? '+' : ''}{e.amount}
              </Text>
            </View>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container:     { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  creditCard:    { backgroundColor: '#2563EB', borderRadius: 16, padding: 24, marginBottom: 20 },
  label:         { color: '#BFDBFE', fontSize: 14 },
  balance:       { color: '#fff', fontSize: 48, fontWeight: '800' },
  ref:           { color: '#BFDBFE', marginTop: 8 },
  refBold:       { fontWeight: '700', color: '#fff' },
  sectionTitle:  { fontSize: 16, fontWeight: '700', color: '#111827', marginBottom: 10 },
  row:           { flexDirection: 'row', gap: 8, marginBottom: 10 },
  amtBtn:        { flex: 1, borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 8, padding: 10, alignItems: 'center' },
  amtBtnActive:  { backgroundColor: '#2563EB', borderColor: '#2563EB' },
  amtText:       { color: '#374151', fontWeight: '600' },
  amtTextActive: { color: '#fff' },
  input:         { borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 10, padding: 12, fontSize: 16, marginBottom: 10 },
  btn:           { backgroundColor: '#2563EB', borderRadius: 10, padding: 14, alignItems: 'center', marginBottom: 16 },
  btnText:       { color: '#fff', fontWeight: '700', fontSize: 15 },
  txRow:         { flexDirection: 'row', alignItems: 'center', paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
  txType:        { textTransform: 'capitalize', color: '#374151' },
  txDate:        { color: '#9CA3AF', fontSize: 12 },
  txAmount:      { fontSize: 18, fontWeight: '700' },
  positive:      { color: '#16A34A' },
  negative:      { color: '#DC2626' },
});
