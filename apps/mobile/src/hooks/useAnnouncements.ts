import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import { api } from '@/services/api';
import { getCachedAnnouncements } from '@/services/offlineService';

export function useAnnouncements() {
  const activeClubId = useAuthStore((s) => s.activeClubId);
  return useQuery({
    queryKey: ['announcements', activeClubId],
    queryFn:  async () => {
      try {
        return await api.get<{ items: any[] }>(`/clubs/${activeClubId}/announcements`);
      } catch {
        const cached = await getCachedAnnouncements(activeClubId!);
        return { items: cached };
      }
    },
    enabled: !!activeClubId,
  });
}
