import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Image } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { AuthStackParamList } from '@/navigation/AuthNavigator';

type Nav = NativeStackNavigationProp<AuthStackParamList, 'Welcome'>;

export function WelcomeScreen() {
  const nav = useNavigation<Nav>();
  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.title}>RallyHub</Text>
        <Text style={styles.subtitle}>Your sports club, organised.</Text>
      </View>
      <View style={styles.actions}>
        <TouchableOpacity style={styles.btnPrimary} onPress={() => nav.navigate('SignUp')}>
          <Text style={styles.btnPrimaryText}>Create a club</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.btnSecondary} onPress={() => nav.navigate('JoinClub', {})}>
          <Text style={styles.btnSecondaryText}>Join via invite</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => nav.navigate('SignIn')}>
          <Text style={styles.link}>Already have an account? Sign in</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container:       { flex: 1, backgroundColor: '#2563EB', justifyContent: 'space-between', padding: 32 },
  hero:            { flex: 1, justifyContent: 'center', alignItems: 'center' },
  title:           { fontSize: 48, fontWeight: '800', color: '#fff' },
  subtitle:        { fontSize: 18, color: '#BFDBFE', marginTop: 8 },
  actions:         { gap: 12 },
  btnPrimary:      { backgroundColor: '#fff', borderRadius: 12, padding: 16, alignItems: 'center' },
  btnPrimaryText:  { color: '#2563EB', fontWeight: '700', fontSize: 16 },
  btnSecondary:    { borderWidth: 2, borderColor: '#fff', borderRadius: 12, padding: 16, alignItems: 'center' },
  btnSecondaryText:{ color: '#fff', fontWeight: '700', fontSize: 16 },
  link:            { color: '#BFDBFE', textAlign: 'center', marginTop: 8 },
});
