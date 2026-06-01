import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { AuthStackParamList } from '@/navigation/AuthNavigator';
import { signIn } from '@/services/authService';
import { useAuthStore } from '@/store/authStore';
import { api } from '@/services/api';

type Nav = NativeStackNavigationProp<AuthStackParamList, 'SignIn'>;

export function SignInScreen() {
  const nav = useNavigation<Nav>();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSignIn() {
    if (!email || !password) return Alert.alert('Please fill in all fields');
    try {
      setLoading(true);
      const token = await signIn(email, password);
      const user = await api.get<any>('/me');
      setAuth({ isAuthenticated: true, accessToken: token, userId: user.id, email: user.email, displayName: user.displayName, activeClubId: null, activeClubRole: null });
    } catch (e: any) {
      Alert.alert('Sign in failed', e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Sign in</Text>
      <TextInput style={styles.input} placeholder="Email" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" />
      <TextInput style={styles.input} placeholder="Password" value={password} onChangeText={setPassword} secureTextEntry />
      <TouchableOpacity style={styles.btn} onPress={handleSignIn} disabled={loading}>
        {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnText}>Sign in</Text>}
      </TouchableOpacity>
      <TouchableOpacity onPress={() => nav.goBack()}><Text style={styles.link}>Back</Text></TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, justifyContent: 'center', backgroundColor: '#fff' },
  title:     { fontSize: 28, fontWeight: '700', marginBottom: 24, color: '#111827' },
  input:     { borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 10, padding: 14, marginBottom: 12, fontSize: 16 },
  btn:       { backgroundColor: '#2563EB', borderRadius: 10, padding: 16, alignItems: 'center', marginTop: 8 },
  btnText:   { color: '#fff', fontWeight: '700', fontSize: 16 },
  link:      { color: '#2563EB', textAlign: 'center', marginTop: 16 },
});
