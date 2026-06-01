import React from 'react';
import { ScrollView, Text, StyleSheet, ActivityIndicator } from 'react-native';
import { useMyMembership } from '@/hooks/useClub';
import { useUpcomingSessions } from '@/hooks/useSession';
import { useAnnouncements } from '@/hooks/useAnnouncements';
import { useAuthStore } from '@/store/authStore';
import { CreditCard } from '@/components/common/CreditCard';
import { SessionCard } from '@/components/session/SessionCard';
import { useNavigation } from '@react-navigation/native';

export function DashboardScreen() {
  const nav  = useNavigation<any>();
  const name = useAuthStore((s) => s.displayName);

  const { data: membership }     = useMyMembership();
  const { data: sessionsData }   = useUpcomingSessions();
  const { data: announcementsData } = useAnnouncements();

  const sessions      = sessionsData?.items ?? [];
  const announcements = announcementsData?.items ?? [];

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ paddingBottom: 32 }}>
      <Text style={styles.greeting}>Hey, {name} 👋</Text>

      <CreditCard
        balance={membership?.creditBalance ?? null}
        paymentReference={membership?.paymentReference}
        onTopUp={() => nav.navigate('Wallet')}
      />

      <Text style={styles.sectionTitle}>Upcoming sessions</Text>
      {sessions.slice(0, 3).map((s: any) => (
        <SessionCard key={s.id} session={s}
          onBook={undefined} onCancel={undefined} onWaitlist={undefined}
        />
      ))}

      <Text style={styles.sectionTitle}>Announcements</Text>
      {announcements.map((a: any) => (
        <Text key={a.id} style={styles.announcement}>
          {a.pinned ? '📌 ' : ''}{a.body}
        </Text>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  greeting:     { fontSize: 22, fontWeight: '700', color: '#111827', marginBottom: 16 },
  sectionTitle: { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 10, marginTop: 8 },
  announcement: { backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 8, color: '#374151' },
});
