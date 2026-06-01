import React, { useState } from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity, TextInput, Alert, ActivityIndicator } from 'react-native';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import type { ClubRole } from '@rallyhub/shared';

export function OrgMembersScreen() {
  const { activeClubId } = useAuthStore();
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const { data, isLoading } = useQuery({ queryKey: ['members', activeClubId], queryFn: () => api.get<any>(`/clubs/${activeClubId}/members`) });

  const promote = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: ClubRole }) =>
      api.patch(`/clubs/${activeClubId}/members/${userId}/role`, { role }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['members', activeClubId] }),
    onError: (e: any) => Alert.alert('Error', e.message),
  });

  const adjustCredit = useMutation({
    mutationFn: ({ userId, amount, note }: { userId: string; amount: number; note: string }) =>
      api.post(`/clubs/${activeClubId}/credits/${userId}/adjust`, { amount, note }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['members', activeClubId] }),
  });

  const filtered = (data?.items ?? []).filter((m: any) => m.displayName.toLowerCase().includes(search.toLowerCase()));

  function creditColor(balance: number) {
    if (balance <= -2) return '#DC2626';
    if (balance < 2)  return '#F59E0B';
    return '#16A34A';
  }

  return (
    <View style={styles.container}>
      <Text style={styles.header}>Members</Text>
      <TextInput style={styles.search} placeholder="Search members..." value={search} onChangeText={setSearch} />
      {isLoading ? <ActivityIndicator color="#2563EB" /> : (
        <FlatList
          data={filtered}
          keyExtractor={(m) => m.userId}
          renderItem={({ item: m }) => (
            <View style={styles.card}>
              <View style={styles.avatar}><Text style={styles.avatarText}>{m.displayName.charAt(0)}</Text></View>
              <View style={{ flex: 1 }}>
                <Text style={styles.name}>{m.displayName}</Text>
                <Text style={styles.role}>{m.role.replace(/_/g, ' ')}</Text>
              </View>
              <Text style={[styles.credits, { color: creditColor(m.creditBalance) }]}>{m.creditBalance}</Text>
              {m.role === 'inductee' && m.healthDeclarationSubmitted && (
                <TouchableOpacity style={styles.promoteBtn} onPress={() => promote.mutate({ userId: m.userId, role: 'full_member' })}>
                  <Text style={styles.promoteBtnText}>Promote</Text>
                </TouchableOpacity>
              )}
            </View>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container:      { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  header:         { fontSize: 22, fontWeight: '700', color: '#111827', marginBottom: 12 },
  search:         { borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 10, padding: 12, marginBottom: 12, backgroundColor: '#fff' },
  card:           { backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 8, flexDirection: 'row', alignItems: 'center', gap: 12 },
  avatar:         { width: 40, height: 40, borderRadius: 20, backgroundColor: '#2563EB', justifyContent: 'center', alignItems: 'center' },
  avatarText:     { color: '#fff', fontWeight: '700' },
  name:           { fontWeight: '600', color: '#111827' },
  role:           { color: '#6B7280', fontSize: 12, textTransform: 'capitalize' },
  credits:        { fontSize: 20, fontWeight: '800', width: 36, textAlign: 'right' },
  promoteBtn:     { backgroundColor: '#DCFCE7', borderRadius: 8, paddingHorizontal: 10, paddingVertical: 6, marginLeft: 8 },
  promoteBtnText: { color: '#16A34A', fontWeight: '700', fontSize: 12 },
});
