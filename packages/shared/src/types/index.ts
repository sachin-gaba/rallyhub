// ─── Roles ────────────────────────────────────────────────────────
export type PlatformRole = 'platform_admin';

export type ClubRole =
  | 'organizer_primary'
  | 'organizer_additional'
  | 'co_organizer'
  | 'full_member'
  | 'inductee';

// ─── User ─────────────────────────────────────────────────────────
export interface User {
  id: string;
  email: string;
  displayName: string;
  phone?: string;
  createdAt: string;
  platformRole?: PlatformRole;
}

// ─── Skill Level ──────────────────────────────────────────────────
export type SkillTier = 'beginner' | 'improver' | 'intermediate' | 'advanced' | 'elite';

export interface SkillLevel {
  tier: SkillTier;
  label: string; // club can rename e.g. 'County'
}

// ─── Club ─────────────────────────────────────────────────────────
export interface Club {
  id: string;
  name: string;
  sports: string[];
  primaryVenue: string;
  welcomeMessage?: string;
  rulesDocument?: string;
  bankTransferInstructions?: string;
  inviteCode: string;
  plan: PlanType;
  planExpiresAt?: string;
  createdAt: string;
  settings: ClubSettings;
}

export interface ClubSettings {
  negativeCreditLimit: number;         // default: -2
  guestSponsorLimitPerMonth: number;   // default: 2
  waitlistSequentialWindowHours: number; // default: 2
  panicWindowHoursBeforeSession: number; // default: 24
  cancellationPolicy: CancellationPolicy;
  notificationPreferences: ClubNotificationDefaults;
}

export interface CancellationPolicy {
  tiers: CancellationTier[];
}

export interface CancellationTier {
  hoursBeforeSession: number;
  refundPercent: number; // 0–100
}

export interface ClubNotificationDefaults {
  sessionReminderHours: number[]; // e.g. [24, 2]
}

// ─── Club Membership ──────────────────────────────────────────────
export interface ClubMembership {
  userId: string;
  clubId: string;
  role: ClubRole;
  creditBalance: number;
  paymentReference: string;
  skillLevel?: SkillTier;
  joinedAt: string;
  inductionCompleted: boolean;
  healthDeclarationId?: string;
}

// ─── Schedule & Session ───────────────────────────────────────────
export interface Schedule {
  id: string;
  clubId: string;
  name: string;
  dayOfWeek: number; // 0=Sun … 6=Sat
  startTime: string; // HH:mm
  venue: string;
  capacityLimit: number;
  reducedCapacity?: number;
  priceCredits: number;
  isInductionSchedule: boolean;
  createdAt: string;
}

export type SessionStatus =
  | 'scheduled'
  | 'in_progress'
  | 'completed'
  | 'cancelled';

export interface Session {
  id: string;
  scheduleId: string;
  clubId: string;
  date: string;        // ISO date
  status: SessionStatus;
  attendees: SessionAttendee[];
  waitlist: WaitlistEntry[];
  guestLedger: GuestLedgerEntry[];
  cancelledReason?: string;
  createdAt: string;
}

export interface SessionAttendee {
  userId: string;
  bookedAt: string;
  attended?: boolean;
  creditsDeducted?: number;
}

export interface WaitlistEntry {
  userId: string;
  joinedAt: string;
  position: number;
  notifiedAt?: string;
  windowExpiresAt?: string;
}

export interface GuestLedgerEntry {
  id: string;
  guestName: string;
  sponsorUserId: string;
  sessionFee: number;
  paymentMethod: 'cash_collected' | 'bank_transfer_pending' | 'complimentary';
  note?: string;
  createdAt: string;
}

// ─── Credit Ledger ────────────────────────────────────────────────
export type LedgerEntryType =
  | 'top_up'
  | 'session_deduction'
  | 'cancellation_refund'
  | 'organizer_cancellation_refund'
  | 'correction'
  | 'negative_limit_release';

export interface CreditLedgerEntry {
  id: string;
  userId: string;
  clubId: string;
  type: LedgerEntryType;
  amount: number;         // positive = credit, negative = debit
  balanceAfter: number;
  note?: string;
  createdBy: string;      // userId of actor
  sessionId?: string;
  createdAt: string;
}

// ─── Tournament ───────────────────────────────────────────────────
export type TournamentFormat = 'group_knockout' | 'round_robin';
export type DrawType = 'random' | 'skill_matched' | 'mixed_ability';

export interface Tournament {
  id: string;
  clubId: string;
  name: string;
  format: TournamentFormat;
  drawType: DrawType;
  sport: string;
  participants: TournamentParticipant[];
  groups?: TournamentGroup[];
  matches: TournamentMatch[];
  tiebreakerOrder: TiebreakerRule[];
  createdAt: string;
}

export interface TournamentParticipant {
  userId: string;
  skillTier: SkillTier;
  groupId?: string;
}

export interface TournamentGroup {
  id: string;
  name: string;
  participantIds: string[];
}

export type MatchStatus = 'scheduled' | 'score_submitted' | 'disputed' | 'confirmed' | 'resolved';

export interface TournamentMatch {
  id: string;
  tournamentId: string;
  round: string;       // e.g. 'group_a', 'qf', 'sf', 'final'
  player1Id: string;
  player2Id: string;
  scorePlayer1?: number;
  scorePlayer2?: number;
  submittedBy?: string;
  status: MatchStatus;
  disputeNote?: string;
  resolvedBy?: string;
  scheduledAt?: string;
  completedAt?: string;
}

export type TiebreakerRule =
  | 'wins'
  | 'head_to_head'
  | 'points_difference'
  | 'total_points'
  | 'coin_flip';

// ─── Health Declaration ───────────────────────────────────────────
export interface HealthDeclaration {
  id: string;
  userId: string;
  clubId: string;
  parqAnswers: Record<number, boolean>;  // Q1-Q7: false=No, true=Yes
  medicalConditions: string[];
  medications: string;
  emergencyContact: EmergencyContact;
  liabilityAccepted: boolean;
  dataProtectionConsented: boolean;
  submittedAt: string;
  deviceInfo: string;
  appVersion: string;
  pdfUrl?: string;
}

export interface EmergencyContact {
  fullName: string;
  relationship: string;
  primaryPhone: string;
  secondaryPhone?: string;
}

// ─── Notifications ────────────────────────────────────────────────
export type NotificationEvent =
  | 'low_credit'
  | 'credit_below_negative_limit'
  | 'payment_mark_as_paid'
  | 'credit_adjusted'
  | 'waitlist_slot_sequential'
  | 'waitlist_panic_window'
  | 'session_cancelled'
  | 'join_request_accepted'
  | 'join_request_declined'
  | 'new_join_request'
  | 'match_result_submitted'
  | 'match_result_disputed'
  | 'session_reminder'
  | 'health_declaration_submitted'
  | 'promotion_ready';

// ─── Plans ────────────────────────────────────────────────────────
export type PlanType = 'starter' | 'club' | 'multi_club';

export interface Plan {
  type: PlanType;
  memberCap: number;
  schedulesCap: number;
  priceMonthly: number; // GBP
}

// ─── API response wrappers ────────────────────────────────────────
export interface ApiResponse<T> {
  data: T;
  message?: string;
}

export interface ApiError {
  code: string;
  message: string;
  details?: unknown;
}

export interface PaginatedResponse<T> {
  items: T[];
  nextToken?: string;
  count: number;
}
