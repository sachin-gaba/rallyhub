import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

type Variant = 'booked' | 'open' | 'waitlist' | 'cancelled' | 'completed' | 'pending' | 'inductee' | 'full_member';

const COLORS: Record<Variant, { bg: string; text: string }> = {
  booked:      { bg: '#DCFCE7', text: '#166534' },
  open:        { bg: '#FEF3C7', text: '#92400E' },
  waitlist:    { bg: '#EFF6FF', text: '#1D4ED8' },
  cancelled:   { bg: '#FEE2E2', text: '#991B1B' },
  completed:   { bg: '#F3F4F6', text: '#374151' },
  pending:     { bg: '#FEF3C7', text: '#92400E' },
  inductee:    { bg: '#FEF3C7', text: '#92400E' },
  full_member: { bg: '#DCFCE7', text: '#166534' },
};

interface Props { variant: Variant; label?: string; }

export function StatusPill({ variant, label }: Props) {
  const c = COLORS[variant] ?? { bg: '#F3F4F6', text: '#374151' };
  return (
    <View style={[styles.pill, { backgroundColor: c.bg }]}>
      <Text style={[styles.text, { color: c.text }]}>{label ?? variant.replace(/_/g, ' ')}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  pill: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 20, alignSelf: 'flex-start' },
  text: { fontSize: 12, fontWeight: '600', textTransform: 'capitalize' },
});
