import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert } from 'react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import type { NativeStackNavigationProp, RouteProp } from '@react-navigation/native-stack';
import type { AuthStackParamList } from '@/navigation/AuthNavigator';
import { confirmSignUp } from '@/services/authService';

type Nav   = NativeStackNavigationProp<AuthStackParamList, 'VerifyEmail'>;
type Route = RouteProp<AuthStackParamList, 'VerifyEmail'>;

export function VerifyEmailScreen() {
  const nav   = useNavigation<Nav>();
  const route = useRoute<Route>();
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleVerify() {
    if (!code) return;
    try {
      setLoading(true);
      await confirmSignUp(route.params.email, code);
      nav.navigate('SignIn');
    } catch (e: any) {
      Alert.alert('Verification failed', e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Check your email</Text>
      <Text style={styles.sub}>We sent a code to {route.params.email}</Text>
      <TextInput style={styles.input} placeholder="6-digit code" value={code} onChangeText={setCode} keyboardType="number-pad" />
      <TouchableOpacity style={styles.btn} onPress={handleVerify} disabled={loading}>
        {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnText}>Verify</Text>}
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, justifyContent: 'center', backgroundColor: '#fff' },
  title:     { fontSize: 28, fontWeight: '700', marginBottom: 8, color: '#111827' },
  sub:       { color: '#6B7280', marginBottom: 24 },
  input:     { borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 10, padding: 14, marginBottom: 12, fontSize: 20, textAlign: 'center', letterSpacing: 8 },
  btn:       { backgroundColor: '#2563EB', borderRadius: 10, padding: 16, alignItems: 'center' },
  btnText:   { color: '#fff', fontWeight: '700', fontSize: 16 },
});
