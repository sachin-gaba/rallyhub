import { create } from 'zustand';
import type { Club, ClubMembership } from '@rallyhub/shared';

interface ClubState {
  clubs: Club[];
  memberships: ClubMembership[];
  activeClub: Club | null;
  setClubs: (clubs: Club[]) => void;
  setMemberships: (memberships: ClubMembership[]) => void;
  setActiveClub: (club: Club) => void;
}

export const useClubStore = create<ClubState>((set) => ({
  clubs: [],
  memberships: [],
  activeClub: null,
  setClubs: (clubs) => set({ clubs }),
  setMemberships: (memberships) => set({ memberships }),
  setActiveClub: (club) => set({ activeClub: club }),
}));
