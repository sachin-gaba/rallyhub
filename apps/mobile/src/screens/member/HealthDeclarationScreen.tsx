import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, TextInput, Alert, ActivityIndicator } from 'react-native';
import { useMutation } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';

const PAR_Q = [
  'Has a doctor ever said you have a heart condition and should only do physical activity recommended by a doctor?',
  'Do you feel pain in your chest when you do physical activity?',
  'In the past month, have you had chest pain when NOT doing physical activity?',
  'Do you lose your balance because of dizziness, or ever lose consciousness?',
  'Do you have a bone, joint, or soft-tissue problem that could be made worse by changed physical activity?',
  'Is your doctor currently prescribing medication for blood pressure or a heart condition?',
  'Do you know of any other reason why you should not participate in physical activity?',
];

const CONDITIONS = [
  'Cardiovascular disease',
  'Respiratory condition',
  'Musculoskeletal condition or recent injury',
  'Neurological condition',
  'Metabolic condition',
  'Recent surgery (within last 6 months)',
  'Pregnancy or recent childbirth (within last 6 months)',
  'Diagnosed mental health condition',
  'None of the above',
];

export function HealthDeclarationScreen() {
  const { activeClubId } = useAuthStore();
  const [step, setStep] = useState(0);
  const [parq, setParq] = useState<Record<number, boolean>>({});
  const [conditions, setConditions] = useState<string[]>([]);
  const [medications, setMedications] = useState('');
  const [ecName, setEcName] = useState('');
  const [ecRel, setEcRel] = useState('');
  const [ecPhone, setEcPhone] = useState('');
  const [waiverAccepted, setWaiverAccepted] = useState(false);
  const [dataConsent, setDataConsent] = useState(false);
  const [confirmEmail, setConfirmEmail] = useState('');

  const submit = useMutation({
    mutationFn: () => api.post(`/clubs/${activeClubId}/health-declaration`, {
      parqAnswers: parq, medicalConditions: conditions, medications,
      emergencyContact: { fullName: ecName, relationship: ecRel, primaryPhone: ecPhone },
      liabilityAccepted: waiverAccepted, dataProtectionConsented: dataConsent,
    }),
    onSuccess: () => Alert.alert('Declaration submitted', 'Your organizer has been notified.'),
    onError: (e: any) => Alert.alert('Error', e.message),
  });

  const steps = [
    // Step 0 — PAR-Q
    <ScrollView key="parq" contentContainerStyle={{ paddingBottom: 20 }}>
      <Text style={styles.stepTitle}>Part 1: Physical Activity Readiness</Text>
      {PAR_Q.map((q, i) => (
        <View key={i} style={styles.qCard}>
          <Text style={styles.qText}>{q}</Text>
          <View style={styles.yesNo}>
            <TouchableOpacity style={[styles.toggle, parq[i] === false && styles.toggleNo]} onPress={() => setParq({ ...parq, [i]: false })}>
              <Text style={[styles.toggleText, parq[i] === false && styles.toggleTextActive]}>NO</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[styles.toggle, parq[i] === true && styles.toggleYes]} onPress={() => setParq({ ...parq, [i]: true })}>
              <Text style={[styles.toggleText, parq[i] === true && styles.toggleTextActive]}>YES</Text>
            </TouchableOpacity>
          </View>
        </View>
      ))}
      {Object.values(parq).some(Boolean) && (
        <View style={styles.advisory}><Text style={styles.advisoryText}>⚠ Please consult your doctor before participating. You may still join, but by continuing you confirm you have or will seek medical advice.</Text></View>
      )}
    </ScrollView>,
    // Step 1 — Conditions
    <ScrollView key="conditions" contentContainerStyle={{ paddingBottom: 20 }}>
      <Text style={styles.stepTitle}>Part 2: Medical History</Text>
      {CONDITIONS.map((c) => {
        const sel = conditions.includes(c);
        return (
          <TouchableOpacity key={c} style={[styles.condCard, sel && styles.condCardSel]} onPress={() => setConditions(sel ? conditions.filter((x) => x !== c) : [...conditions.filter((x) => x !== 'None of the above'), c])}>
            <Text style={[styles.condText, sel && styles.condTextSel]}>{c}</Text>
          </TouchableOpacity>
        );
      })}
    </ScrollView>,
    // Step 2 — Medications
    <View key="meds" style={{ flex: 1 }}>
      <Text style={styles.stepTitle}>Part 3: Current Medications</Text>
      <Text style={styles.stepSub}>List any medications that may affect your ability to exercise safely. Leave blank if none.</Text>
      <TextInput style={[styles.input, { height: 120 }]} multiline placeholder="e.g. beta-blockers, blood thinners, insulin..." value={medications} onChangeText={setMedications} />
    </View>,
    // Step 3 — Emergency contact
    <View key="ec" style={{ flex: 1 }}>
      <Text style={styles.stepTitle}>Part 4: Emergency Contact</Text>
      <TextInput style={styles.input} placeholder="Full name" value={ecName} onChangeText={setEcName} />
      <TextInput style={styles.input} placeholder="Relationship (e.g. spouse)" value={ecRel} onChangeText={setEcRel} />
      <TextInput style={styles.input} placeholder="Phone number" value={ecPhone} onChangeText={setEcPhone} keyboardType="phone-pad" />
    </View>,
    // Step 4 — Waiver
    <ScrollView key="waiver" contentContainerStyle={{ paddingBottom: 20 }}>
      <Text style={styles.stepTitle}>Part 5: Liability Waiver</Text>
      <View style={styles.waiverBox}>
        <Text style={styles.waiverText}>I voluntarily choose to participate in sports activities organised by this club. I understand that participation carries inherent risks of injury, including muscular strains, joint injuries, fractures, cardiovascular events, and in extreme cases permanent disability or death.{'\n\n'}I confirm that the health information I have provided is accurate and complete. I release and discharge the club, its organisers, co-organisers, volunteers, and the platform operator from any and all liability except in cases of gross negligence or wilful misconduct.{'\n\n'}I confirm I am 18+ or have obtained written parental consent.</Text>
      </View>
      <TouchableOpacity style={styles.checkRow} onPress={() => setWaiverAccepted(!waiverAccepted)}>
        <View style={[styles.checkbox, waiverAccepted && styles.checkboxChecked]}>{waiverAccepted && <Text style={{ color: '#fff' }}>✓</Text>}</View>
        <Text style={styles.checkText}>I accept the above terms</Text>
      </TouchableOpacity>
    </ScrollView>,
    // Step 5 — Data consent + submit
    <View key="consent" style={{ flex: 1 }}>
      <Text style={styles.stepTitle}>Part 6 & 7: Consent & Submit</Text>
      <TouchableOpacity style={styles.checkRow} onPress={() => setDataConsent(!dataConsent)}>
        <View style={[styles.checkbox, dataConsent && styles.checkboxChecked]}>{dataConsent && <Text style={{ color: '#fff' }}>✓</Text>}</View>
        <Text style={styles.checkText}>I consent to the club and platform storing my health declaration for member safety management.</Text>
      </TouchableOpacity>
      <TextInput style={styles.input} placeholder="Confirm your email address" value={confirmEmail} onChangeText={setConfirmEmail} autoCapitalize="none" keyboardType="email-address" />
      <TouchableOpacity style={[styles.btn, (!waiverAccepted || !dataConsent || !confirmEmail) && { opacity: 0.4 }]} disabled={!waiverAccepted || !dataConsent || !confirmEmail || submit.isPending} onPress={() => submit.mutate()}>
        {submit.isPending ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnText}>Submit declaration</Text>}
      </TouchableOpacity>
    </View>,
  ];

  return (
    <View style={styles.container}>
      <View style={styles.progress}>
        {steps.map((_, i) => <View key={i} style={[styles.dot, i <= step && styles.dotActive]} />)}
      </View>
      <View style={{ flex: 1 }}>{steps[step]}</View>
      <View style={styles.navRow}>
        {step > 0 && <TouchableOpacity style={styles.backBtn} onPress={() => setStep(step - 1)}><Text style={styles.backText}>Back</Text></TouchableOpacity>}
        {step < steps.length - 1 && <TouchableOpacity style={styles.nextBtn} onPress={() => setStep(step + 1)}><Text style={styles.nextText}>Next</Text></TouchableOpacity>}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container:       { flex: 1, backgroundColor: '#F9FAFB', padding: 20 },
  progress:        { flexDirection: 'row', gap: 6, marginBottom: 16, justifyContent: 'center' },
  dot:             { width: 8, height: 8, borderRadius: 4, backgroundColor: '#D1D5DB' },
  dotActive:       { backgroundColor: '#2563EB' },
  stepTitle:       { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 6 },
  stepSub:         { color: '#6B7280', marginBottom: 12 },
  qCard:           { backgroundColor: '#fff', borderRadius: 10, padding: 14, marginBottom: 8 },
  qText:           { color: '#374151', marginBottom: 8 },
  yesNo:           { flexDirection: 'row', gap: 8 },
  toggle:          { flex: 1, borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 8, padding: 10, alignItems: 'center' },
  toggleYes:       { backgroundColor: '#FEE2E2', borderColor: '#EF4444' },
  toggleNo:        { backgroundColor: '#DCFCE7', borderColor: '#16A34A' },
  toggleText:      { fontWeight: '700', color: '#6B7280' },
  toggleTextActive:{ color: '#111827' },
  advisory:        { backgroundColor: '#FEF3C7', borderRadius: 10, padding: 14, marginTop: 8 },
  advisoryText:    { color: '#92400E' },
  condCard:        { backgroundColor: '#fff', borderRadius: 10, padding: 14, marginBottom: 6 },
  condCardSel:     { backgroundColor: '#EFF6FF', borderWidth: 1, borderColor: '#2563EB' },
  condText:        { color: '#374151' },
  condTextSel:     { color: '#2563EB', fontWeight: '600' },
  input:           { borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 10, padding: 12, fontSize: 15, marginBottom: 10, backgroundColor: '#fff' },
  waiverBox:       { backgroundColor: '#fff', borderRadius: 10, padding: 16, marginBottom: 12 },
  waiverText:      { color: '#374151', lineHeight: 20 },
  checkRow:        { flexDirection: 'row', gap: 12, alignItems: 'flex-start', marginBottom: 12 },
  checkbox:        { width: 24, height: 24, borderRadius: 6, borderWidth: 2, borderColor: '#D1D5DB', justifyContent: 'center', alignItems: 'center' },
  checkboxChecked: { backgroundColor: '#2563EB', borderColor: '#2563EB' },
  checkText:       { flex: 1, color: '#374151' },
  btn:             { backgroundColor: '#2563EB', borderRadius: 10, padding: 16, alignItems: 'center', marginTop: 8 },
  btnText:         { color: '#fff', fontWeight: '700', fontSize: 16 },
  navRow:          { flexDirection: 'row', justifyContent: 'space-between', marginTop: 12 },
  backBtn:         { borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 8, paddingHorizontal: 20, paddingVertical: 10 },
  backText:        { color: '#374151' },
  nextBtn:         { backgroundColor: '#2563EB', borderRadius: 8, paddingHorizontal: 24, paddingVertical: 10, marginLeft: 'auto' },
  nextText:        { color: '#fff', fontWeight: '700' },
});
