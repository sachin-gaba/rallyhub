import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import { api } from '@/services/api';
import { getCachedSessions } from '@/services/offlineService';
import type { Session } from '@rallyhub/shared';

export function useUpcomingSessions() {
  const activeClubId = useAuthStore((s) => s.activeClubId);
  return useQuery({
    queryKey: ['sessions', activeClubId, 'upcoming'],
    queryFn:  async () => {
      try {
        return await api.get<{ items: any[] }>(`/clubs/${activeClubId}/sessions?upcoming=true`);
      } catch {
        // Offline fallback: serve SQLite cache
        const cached = await getCachedSessions(activeClubId!);
        return { items: cached };
      }
    },
    enabled: !!activeClubId,
  });
}

export function useBookSession() {
  const qc           = useQueryClient();
  const activeClubId = useAuthStore((s) => s.activeClubId);
  return useMutation({
    mutationFn: (sessionId: string) =>
      api.post(`/clubs/${activeClubId}/sessions/${sessionId}/book`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sessions', activeClubId] }),
  });
}

export function useCancelBooking() {
  const qc           = useQueryClient();
  const activeClubId = useAuthStore((s) => s.activeClubId);
  return useMutation({
    mutationFn: (sessionId: string) =>
      api.delete(`/clubs/${activeClubId}/sessions/${sessionId}/book`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sessions', activeClubId] }),
  });
}
