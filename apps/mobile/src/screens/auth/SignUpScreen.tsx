import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert, ScrollView } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { AuthStackParamList } from '@/navigation/AuthNavigator';
import { signUp } from '@/services/authService';

type Nav = NativeStackNavigationProp<AuthStackParamList, 'SignUp'>;

export function SignUpScreen() {
  const nav = useNavigation<Nav>();
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSignUp() {
    if (!displayName || !email || !password) return Alert.alert('Please fill in all fields');
    try {
      setLoading(true);
      await signUp(email, password, displayName);
      nav.navigate('VerifyEmail', { email });
    } catch (e: any) {
      Alert.alert('Sign up failed', e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>Create account</Text>
      <TextInput style={styles.input} placeholder="Display name" value={displayName} onChangeText={setDisplayName} />
      <TextInput style={styles.input} placeholder="Email" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" />
      <TextInput style={styles.input} placeholder="Password (min 8 chars)" value={password} onChangeText={setPassword} secureTextEntry />
      <TouchableOpacity style={styles.btn} onPress={handleSignUp} disabled={loading}>
        {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnText}>Create account</Text>}
      </TouchableOpacity>
      <TouchableOpacity onPress={() => nav.goBack()}><Text style={styles.link}>Back</Text></TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flexGrow: 1, padding: 24, justifyContent: 'center', backgroundColor: '#fff' },
  title:     { fontSize: 28, fontWeight: '700', marginBottom: 24, color: '#111827' },
  input:     { borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 10, padding: 14, marginBottom: 12, fontSize: 16 },
  btn:       { backgroundColor: '#2563EB', borderRadius: 10, padding: 16, alignItems: 'center', marginTop: 8 },
  btnText:   { color: '#fff', fontWeight: '700', fontSize: 16 },
  link:      { color: '#2563EB', textAlign: 'center', marginTop: 16 },
});
