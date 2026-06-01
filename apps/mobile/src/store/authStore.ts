import { create } from 'zustand';
import type { ClubRole } from '@rallyhub/shared';

interface AuthState {
  isAuthenticated: boolean;
  userId: string | null;
  email: string | null;
  displayName: string | null;
  accessToken: string | null;
  activeClubId: string | null;
  activeClubRole: ClubRole | null;
  setAuth: (payload: Omit<AuthState, 'setAuth' | 'clearAuth' | 'setActiveClub'>) => void;
  setActiveClub: (clubId: string, role: ClubRole) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: false,
  userId: null,
  email: null,
  displayName: null,
  accessToken: null,
  activeClubId: null,
  activeClubRole: null,
  setAuth: (payload) => set({ ...payload }),
  setActiveClub: (clubId, role) => set({ activeClubId: clubId, activeClubRole: role }),
  clearAuth: () =>
    set({
      isAuthenticated: false,
      userId: null,
      email: null,
      displayName: null,
      accessToken: null,
      activeClubId: null,
      activeClubRole: null,
    }),
}));
