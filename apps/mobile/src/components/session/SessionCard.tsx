import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { StatusPill } from '../common/StatusPill';

interface Props {
  session: {
    id: string;
    scheduleName: string;
    date: string;
    venue?: string;
    attendeeCount: number;
    capacity: number;
    priceCredits: number;
    booked: boolean;
    waitlisted?: boolean;
    status: string;
  };
  onBook?:     () => void;
  onCancel?:   () => void;
  onWaitlist?: () => void;
}

export function SessionCard({ session: s, onBook, onCancel, onWaitlist }: Props) {
  const isFull = s.attendeeCount >= s.capacity;
  const variant = s.booked ? 'booked' : s.waitlisted ? 'waitlist' : isFull ? 'waitlist' : 'open';

  return (
    <View style={styles.card}>
      <View style={{ flex: 1 }}>
        <Text style={styles.date}>
          {new Date(s.date).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })}
        </Text>
        <Text style={styles.name}>{s.scheduleName}</Text>
        <Text style={styles.meta}>
          {s.venue} · {s.attendeeCount}/{s.capacity} · {s.priceCredits} credit{s.priceCredits !== 1 ? 's' : ''}
        </Text>
      </View>
      <View style={styles.right}>
        <StatusPill variant={variant as any} />
        {s.booked && onCancel && (
          <TouchableOpacity style={styles.cancelBtn} onPress={onCancel}>
            <Text style={styles.cancelText}>Cancel</Text>
          </TouchableOpacity>
        )}
        {!s.booked && !isFull && onBook && (
          <TouchableOpacity style={styles.bookBtn} onPress={onBook}>
            <Text style={styles.bookText}>Book</Text>
          </TouchableOpacity>
        )}
        {!s.booked && isFull && onWaitlist && (
          <TouchableOpacity style={styles.waitBtn} onPress={onWaitlist}>
            <Text style={styles.waitText}>Waitlist</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card:      { backgroundColor: '#fff', borderRadius: 12, padding: 16, marginBottom: 8, flexDirection: 'row', alignItems: 'center', borderLeftWidth: 4, borderLeftColor: '#2563EB' },
  date:      { color: '#6B7280', fontSize: 12 },
  name:      { fontWeight: '600', color: '#111827', fontSize: 15, marginVertical: 2 },
  meta:      { color: '#9CA3AF', fontSize: 12 },
  right:     { alignItems: 'flex-end', gap: 6 },
  bookBtn:   { backgroundColor: '#2563EB', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 6 },
  bookText:  { color: '#fff', fontWeight: '700', fontSize: 13 },
  cancelBtn: { backgroundColor: '#FEE2E2', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 6 },
  cancelText:{ color: '#DC2626', fontWeight: '700', fontSize: 13 },
  waitBtn:   { backgroundColor: '#FEF3C7', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 6 },
  waitText:  { color: '#92400E', fontWeight: '700', fontSize: 13 },
});
