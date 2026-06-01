export const DEFAULT_NEGATIVE_CREDIT_LIMIT = -2;
export const DEFAULT_GUEST_SPONSOR_LIMIT_PER_MONTH = 2;
export const DEFAULT_WAITLIST_SEQUENTIAL_WINDOW_HOURS = 2;
export const DEFAULT_PANIC_WINDOW_HOURS_BEFORE_SESSION = 24;
export const DEFAULT_LOW_CREDIT_THRESHOLD = 2;

export const DEFAULT_CANCELLATION_POLICY = [
  { hoursBeforeSession: 48, refundPercent: 100 },
  { hoursBeforeSession: 24, refundPercent: 50 },
  { hoursBeforeSession: 0,  refundPercent: 0 },
];

export const SKILL_TIER_LABELS: Record<string, string> = {
  beginner:     'Beginner',
  improver:     'Improver',
  intermediate: 'Intermediate',
  advanced:     'Advanced',
  elite:        'Elite',
};

export const PLAN_DEFAULTS = {
  starter:    { memberCap: 20,  schedulesCap: 1,  priceMonthly: 0    },
  club:       { memberCap: 60,  schedulesCap: 3,  priceMonthly: 9.99 },
  multi_club: { memberCap: 200, schedulesCap: 10, priceMonthly: 24.99 },
};

export const STARTER_FREE_PERIOD_DAYS = 90;
export const STARTER_GRACE_PERIOD_DAYS = 14;
export const STARTER_LOCKED_BEFORE_DELETION_DAYS = 180;

export const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL ?? 'https://api.rallyhub.app';
