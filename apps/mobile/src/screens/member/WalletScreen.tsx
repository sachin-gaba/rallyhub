import React, { useState } from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity, TextInput, Alert, ActivityIndicator } from 'react-native';
import { useCreditBalance, useLedger, useMarkPaid } from '@/hooks/useCredits';
import { useMyMembership } from '@/hooks/useClub';
import { CreditCard } from '@/components/common/CreditCard';

const AMOUNTS = [5, 10, 20, 50];

export function WalletScreen() {
  const [amount, setAmount] = useState('');
  const { data: balance }   = useCreditBalance();
  const { data: membership }= useMyMembership();
  const { data: ledger, isLoading } = useLedger();
  const markPaid = useMarkPaid();

  return (
    <View style={styles.container}>
      <CreditCard
        balance={balance ?? null}
        paymentReference={membership?.paymentReference}
      />

      <Text style={styles.sectionTitle}>Mark a bank transfer as paid</Text>
      <View style={styles.row}>
        {AMOUNTS.map((a) => (
          <TouchableOpacity key={a} style={[styles.amtBtn, amount === String(a) && styles.amtActive]}
            onPress={() => setAmount(String(a))}>
            <Text style={[styles.amtText, amount === String(a) && styles.amtTextActive]}>£{a}</Text>
          </TouchableOpacity>
        ))}
      </View>
      <TextInput style={styles.input} placeholder="Or enter amount (£)" value={amount}
        onChangeText={setAmount} keyboardType="decimal-pad" />
      <TouchableOpacity style={styles.btn}
        onPress={() => markPaid.mutate(parseFloat(amount), {
          onSuccess: () => { Alert.alert('Submitted', 'The organiser will verify your transfer.'); setAmount(''); },
          onError: (e: any) => Alert.alert('Error', e.message),
        })}
        disabled={!amount || markPaid.isPending}>
        <Text style={styles.btnText}>{markPaid.isPending ? 'Submitting…' : 'Mark as paid'}</Text>
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
              <Text style={[styles.txAmt, { color: e.amount > 0 ? '#16A34A' : '#DC2626' }]}>
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
  container:    { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#111827', marginTop: 16, marginBottom: 8 },
  row:          { flexDirection: 'row', gap: 8, marginBottom: 10 },
  amtBtn:       { flex: 1, borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 8, padding: 10, alignItems: 'center' },
  amtActive:    { backgroundColor: '#2563EB', borderColor: '#2563EB' },
  amtText:      { color: '#374151', fontWeight: '600' },
  amtTextActive:{ color: '#fff' },
  input:        { borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 10, padding: 12, fontSize: 16, marginBottom: 10 },
  btn:          { backgroundColor: '#2563EB', borderRadius: 10, padding: 14, alignItems: 'center', marginBottom: 16 },
  btnText:      { color: '#fff', fontWeight: '700', fontSize: 15 },
  txRow:        { flexDirection: 'row', alignItems: 'center', paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
  txType:       { textTransform: 'capitalize', color: '#374151' },
  txDate:       { color: '#9CA3AF', fontSize: 12 },
  txAmt:        { fontSize: 18, fontWeight: '700' },
});
