import React, { useState } from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity, ActivityIndicator } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';

export function TournamentScreen() {
  const { activeClubId } = useAuthStore();
  const [round, setRound] = useState('group');
  const { data, isLoading } = useQuery({ queryKey: ['tournaments', activeClubId], queryFn: () => api.get<any>(`/clubs/${activeClubId}/tournaments`) });
  const tournament = data?.items?.[0];

  return (
    <View style={styles.container}>
      <Text style={styles.header}>Tournament</Text>
      {isLoading && <ActivityIndicator color="#2563EB" />}
      {tournament ? (
        <>
          <Text style={styles.name}>{tournament.name}</Text>
          <View style={styles.tabs}>
            {['group', 'qf', 'sf', 'final'].map((r) => (
              <TouchableOpacity key={r} style={[styles.tab, round === r && styles.tabActive]} onPress={() => setRound(r)}>
                <Text style={[styles.tabText, round === r && styles.tabTextActive]}>{r.toUpperCase()}</Text>
              </TouchableOpacity>
            ))}
          </View>
          <FlatList
            data={(tournament.matches ?? []).filter((m: any) => m.round === round)}
            keyExtractor={(m) => m.id}
            renderItem={({ item: m }) => (
              <View style={styles.matchCard}>
                <Text style={styles.player}>{m.player1Name}</Text>
                <View style={styles.score}>
                  <Text style={styles.scoreText}>{m.scorePlayer1 ?? '—'}</Text>
                  <Text style={styles.vs}>vs</Text>
                  <Text style={styles.scoreText}>{m.scorePlayer2 ?? '—'}</Text>
                </View>
                <Text style={[styles.player, { textAlign: 'right' }]}>{m.player2Name}</Text>
                {m.status === 'disputed' && <Text style={styles.dispute}>⚠ Disputed</Text>}
              </View>
            )}
          />
        </>
      ) : !isLoading && <Text style={styles.empty}>No active tournament</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  header:       { fontSize: 22, fontWeight: '700', color: '#111827', marginBottom: 4 },
  name:         { color: '#6B7280', marginBottom: 16 },
  tabs:         { flexDirection: 'row', gap: 8, marginBottom: 16 },
  tab:          { flex: 1, borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 8, padding: 8, alignItems: 'center' },
  tabActive:    { backgroundColor: '#2563EB', borderColor: '#2563EB' },
  tabText:      { color: '#374151', fontSize: 12, fontWeight: '700' },
  tabTextActive:{ color: '#fff' },
  matchCard:    { backgroundColor: '#fff', borderRadius: 12, padding: 16, marginBottom: 8, flexDirection: 'row', alignItems: 'center' },
  player:       { flex: 1, fontWeight: '600', color: '#111827', fontSize: 13 },
  score:        { flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 8 },
  scoreText:    { fontSize: 20, fontWeight: '800', color: '#2563EB' },
  vs:           { color: '#9CA3AF', fontSize: 12 },
  dispute:      { color: '#EF4444', fontSize: 11, fontWeight: '600' },
  empty:        { color: '#9CA3AF', textAlign: 'center', marginTop: 40 },
});
