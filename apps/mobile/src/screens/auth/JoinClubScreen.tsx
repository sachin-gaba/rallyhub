import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, ActivityIndicator } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { AuthStackParamList } from '@/navigation/AuthNavigator';
import { api } from '@/services/api';

type Nav = NativeStackNavigationProp<AuthStackParamList, 'JoinClub'>;

export function JoinClubScreen() {
  const nav = useNavigation<Nav>();
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [preview, setPreview] = useState<{ name: string; sport: string } | null>(null);

  async function lookupClub() {
    if (!code.trim()) return;
    try {
      setLoading(true);
      const club = await api.get<any>(`/clubs/invite/${code.trim()}`);
      setPreview({ name: club.name, sport: club.sports.join(', ') });
    } catch {
      Alert.alert('Club not found', 'Check the invite code and try again.');
    } finally {
      setLoading(false);
    }
  }

  async function requestToJoin() {
    try {
      setLoading(true);
      await api.post(`/clubs/invite/${code.trim()}/join`, {});
      Alert.alert('Request sent!', 'The organizer will review your request.');
      nav.navigate('SignIn');
    } catch (e: any) {
      Alert.alert('Error', e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Join a club</Text>
      <TextInput style={styles.input} placeholder="Enter invite code" value={code} onChangeText={setCode} autoCapitalize="characters" />
      {!preview ? (
        <TouchableOpacity style={styles.btn} onPress={lookupClub} disabled={loading}>
          {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnText}>Look up club</Text>}
        </TouchableOpacity>
      ) : (
        <View style={styles.previewCard}>
          <Text style={styles.clubName}>{preview.name}</Text>
          <Text style={styles.clubSport}>{preview.sport}</Text>
          <TouchableOpacity style={styles.btn} onPress={requestToJoin} disabled={loading}>
            {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnText}>Request to join</Text>}
          </TouchableOpacity>
        </View>
      )}
      <TouchableOpacity onPress={() => nav.goBack()}><Text style={styles.link}>Back</Text></TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container:   { flex: 1, padding: 24, justifyContent: 'center', backgroundColor: '#fff' },
  title:       { fontSize: 28, fontWeight: '700', marginBottom: 24, color: '#111827' },
  input:       { borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 10, padding: 14, marginBottom: 12, fontSize: 16 },
  btn:         { backgroundColor: '#2563EB', borderRadius: 10, padding: 16, alignItems: 'center', marginTop: 8 },
  btnText:     { color: '#fff', fontWeight: '700', fontSize: 16 },
  link:        { color: '#2563EB', textAlign: 'center', marginTop: 16 },
  previewCard: { backgroundColor: '#EFF6FF', borderRadius: 12, padding: 20, marginVertical: 12 },
  clubName:    { fontSize: 22, fontWeight: '700', color: '#1E40AF' },
  clubSport:   { color: '#3B82F6', marginBottom: 12 },
});
