package app.rallyhub.service;

import app.rallyhub.domain.model.Club;
import app.rallyhub.domain.repository.ClubRepository;
import app.rallyhub.domain.repository.MembershipRepository;
import app.rallyhub.exception.RallyhubException;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Singleton
public class PlanService {

    // Plan caps (configurable by platform admin — stored in a config table; hardcoded defaults here)
    private static final Map<String, Integer> MEMBER_CAPS   = Map.of("starter", 20,  "club", 60,  "multi_club", 200);
    private static final Map<String, Integer> SCHEDULE_CAPS = Map.of("starter", 1,   "club", 3,   "multi_club", 10);

    private static final int GRACE_PERIOD_DAYS             = 14;
    private static final int LOCKED_BEFORE_DELETION_DAYS   = 180;

    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public PlanService(ClubRepository clubRepository,
                       MembershipRepository membershipRepository,
                       NotificationService notificationService,
                       UserRepository userRepository) {
        this.clubRepository       = clubRepository;
        this.membershipRepository = membershipRepository;
        this.notificationService  = notificationService;
        this.userRepository       = userRepository;
    }

    /**
     * Enforce member cap before adding a new member.
     * Throws 402 (PLAN_LIMIT) if at cap.
     */
    public void enforceMemberCap(String clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> RallyhubException.notFound("Club"));

        enforceNotLocked(club);

        int cap     = MEMBER_CAPS.getOrDefault(club.getPlan(), 20);
        int current = membershipRepository.findByClubId(clubId).size();
        if (current >= cap) {
            throw new RallyhubException("PLAN_LIMIT",
                    "Member cap of " + cap + " reached for the " + club.getPlan() + " plan. Please upgrade.", 402);
        }
        warnIfApproachingCap(club, current, cap);
    }

    /**
     * Enforce schedule cap before creating a new schedule.
     */
    public void enforceScheduleCap(String clubId, int currentScheduleCount) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> RallyhubException.notFound("Club"));
        enforceNotLocked(club);
        int cap = SCHEDULE_CAPS.getOrDefault(club.getPlan(), 1);
        if (currentScheduleCount >= cap)
            throw new RallyhubException("PLAN_LIMIT",
                    "Schedule cap of " + cap + " reached. Please upgrade.", 402);
    }

    /**
     * Check plan expiry status and take appropriate action.
     * Called by daily EventBridge rule.
     */
    public void processPlanExpiry(String clubId) {
        Club club = clubRepository.findById(clubId).orElseThrow();
        if (!"starter".equals(club.getPlan()) || club.getPlanExpiresAt() == null) return;

        Instant now     = Instant.now();
        Instant expiry  = club.getPlanExpiresAt();
        long daysLeft   = now.until(expiry, ChronoUnit.DAYS);

        String organizerUserId = membershipRepository.findByClubId(clubId).stream()
                .filter(m -> "organizer_primary".equals(m.getRole()))
                .findFirst().map(m -> m.getUserId()).orElse(null);
        if (organizerUserId == null) return;

        userRepository.findById(organizerUserId).ifPresent(u -> {
            if (daysLeft == 30) {
                notificationService.send(u.getPushToken(), u.getEmail(), "plan_expiry_warning",
                        "Starter period ends in 30 days",
                        "Upgrade to Club plan to keep full access.");
            } else if (daysLeft == 7) {
                notificationService.send(u.getPushToken(), u.getEmail(), "plan_expiry_warning",
                        "Starter period ends in 7 days",
                        "Upgrade now to avoid service interruption.");
            } else if (daysLeft == 1) {
                notificationService.send(u.getPushToken(), u.getEmail(), "plan_expiry_warning",
                        "Starter period ends tomorrow",
                        "Upgrade today to keep full access.");
            } else if (daysLeft <= 0 && daysLeft > -GRACE_PERIOD_DAYS) {
                // Grace period — already expired, warn daily
                long graceDaysLeft = GRACE_PERIOD_DAYS + daysLeft;
                notificationService.send(u.getPushToken(), u.getEmail(), "plan_grace_period",
                        "Free period ended — " + graceDaysLeft + " days left",
                        "No new members can be added. Upgrade to restore full access.");
                club.setPlan("starter_grace");
                clubRepository.update(club);
            } else if (daysLeft <= -GRACE_PERIOD_DAYS) {
                // Lock the club
                club.setPlan("starter_locked");
                clubRepository.update(club);
                notificationService.send(u.getPushToken(), u.getEmail(), "plan_locked",
                        "Club locked",
                        "Your club is locked. No new bookings or sessions. Upgrade to restore.");
                log.info("Club {} locked due to plan expiry", clubId);
            }
        });
    }

    /**
     * Upgrade club plan (called after Stripe payment confirmed).
     */
    public Club upgradePlan(String clubId, String newPlan) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> RallyhubException.notFound("Club"));
        club.setPlan(newPlan);
        club.setPlanExpiresAt(null); // Paid plans don't expire
        clubRepository.update(club);
        log.info("Club {} upgraded to plan {}", clubId, newPlan);
        return club;
    }

    private void enforceNotLocked(Club club) {
        if ("starter_locked".equals(club.getPlan()))
            throw new RallyhubException("CLUB_LOCKED",
                    "This club is locked. Please upgrade your plan to continue.", 402);
    }

    private void warnIfApproachingCap(Club club, int current, int cap) {
        if (current >= cap * 0.9) {
            membershipRepository.findByClubId(club.getId()).stream()
                    .filter(m -> "organizer_primary".equals(m.getRole()))
                    .findFirst()
                    .ifPresent(m -> userRepository.findById(m.getUserId()).ifPresent(u ->
                            notificationService.send(u.getPushToken(), u.getEmail(),
                                    "plan_cap_warning", "Approaching member cap",
                                    "Your club is at " + current + "/" + cap + " members. Consider upgrading.")));
        }
    }
}
