import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import { api } from '@/services/api';
import type { Club, ClubMembership } from '@rallyhub/shared';

export function useActiveClub() {
  const activeClubId = useAuthStore((s) => s.activeClubId);
  return useQuery<Club>({
    queryKey:  ['club', activeClubId],
    queryFn:   () => api.get<Club>(`/clubs/${activeClubId}`),
    enabled:   !!activeClubId,
    staleTime: 1000 * 60 * 5,
  });
}

export function useMyMembership() {
  const activeClubId = useAuthStore((s) => s.activeClubId);
  return useQuery<ClubMembership>({
    queryKey: ['membership', activeClubId],
    queryFn:  () => api.get<ClubMembership>(`/clubs/${activeClubId}/membership/me`),
    enabled:  !!activeClubId,
  });
}
