import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import { api } from '@/services/api';
import { getCachedBalance } from '@/services/offlineService';
import type { CreditLedgerEntry } from '@rallyhub/shared';

export function useCreditBalance() {
  const activeClubId = useAuthStore((s) => s.activeClubId);
  return useQuery({
    queryKey: ['balance', activeClubId],
    queryFn:  async () => {
      try {
        const m = await api.get<any>(`/clubs/${activeClubId}/membership/me`);
        return m.creditBalance as number;
      } catch {
        return getCachedBalance(activeClubId!);
      }
    },
    enabled: !!activeClubId,
  });
}

export function useLedger() {
  const activeClubId = useAuthStore((s) => s.activeClubId);
  return useQuery<{ items: CreditLedgerEntry[] }>({
    queryKey: ['ledger', activeClubId],
    queryFn:  () => api.get(`/clubs/${activeClubId}/ledger/me`),
    enabled:  !!activeClubId,
  });
}

export function useMarkPaid() {
  const qc           = useQueryClient();
  const activeClubId = useAuthStore((s) => s.activeClubId);
  return useMutation({
    mutationFn: (amount: number) =>
      api.post(`/clubs/${activeClubId}/payments/mark-paid`, { amount }),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['balance', activeClubId] }),
  });
}
