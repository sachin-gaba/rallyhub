import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

interface Props {
  balance: number | null;
  paymentReference?: string;
  onTopUp?: () => void;
}

export function CreditCard({ balance, paymentReference, onTopUp }: Props) {
  const isLow = balance !== null && balance <= 2;
  const isNeg = balance !== null && balance < 0;

  return (
    <View style={[styles.card, isNeg && styles.cardNeg, isLow && !isNeg && styles.cardLow]}>
      <Text style={styles.label}>Credit balance</Text>
      <Text style={styles.value}>{balance ?? '—'}</Text>
      {paymentReference && (
        <Text style={styles.ref}>Ref: <Text style={styles.refBold}>{paymentReference}</Text></Text>
      )}
      {onTopUp && (
        <TouchableOpacity style={styles.btn} onPress={onTopUp}>
          <Text style={styles.btnText}>Top up →</Text>
        </TouchableOpacity>
      )}
      {isLow && <Text style={styles.warning}>{isNeg ? '⚠ Balance overdue' : '⚠ Balance running low'}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  card:     { backgroundColor: '#2563EB', borderRadius: 16, padding: 24 },
  cardLow:  { backgroundColor: '#D97706' },
  cardNeg:  { backgroundColor: '#DC2626' },
  label:    { color: '#BFDBFE', fontSize: 14 },
  value:    { color: '#fff', fontSize: 48, fontWeight: '800', marginVertical: 4 },
  ref:      { color: '#BFDBFE', marginTop: 4 },
  refBold:  { fontWeight: '700', color: '#fff' },
  btn:      { alignSelf: 'flex-end', marginTop: 8 },
  btnText:  { color: '#BFDBFE', fontWeight: '600' },
  warning:  { color: '#FEF3C7', fontSize: 12, marginTop: 6, fontWeight: '600' },
});
