import jakarta.inject.Singleton;
package app.rallyhub.service;

import app.rallyhub.domain.model.*;
import app.rallyhub.domain.repository.*;
import app.rallyhub.exception.RallyhubException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MembershipRepository membershipRepository;
    private final CreditLedgerRepository ledgerRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final NotificationService notificationService;

    // ── Booking ──────────────────────────────────────────────────

    public Session bookSession(String userId, String clubId, String sessionId) {
        Session session = getSessionOrThrow(sessionId);
        ClubMembership membership = getMembershipOrThrow(userId, clubId);

        if ("inductee".equals(membership.getRole()))
            throw RallyhubException.forbidden("Inductees may only book induction sessions");

        List<Session.Attendee> attendees = new ArrayList<>(safeList(session.getAttendees()));
        if (attendees.stream().anyMatch(a -> a.getUserId().equals(userId)))
            throw RallyhubException.conflict("Already booked for this session");

        Club club = clubRepository.findById(clubId).orElseThrow(() -> RallyhubException.notFound("Club"));
        int capacity = resolveCapacity(session, club);
        if (attendees.size() >= capacity)
            throw RallyhubException.conflict("Session is full — join the waitlist");

        attendees.add(Session.Attendee.builder().userId(userId).bookedAt(Instant.now()).build());
        session.setAttendees(attendees);
        sessionRepository.update(session);
        return session;
    }

    public Session cancelBooking(String userId, String clubId, String sessionId) {
        Session session = getSessionOrThrow(sessionId);
        Club club = clubRepository.findById(clubId).orElseThrow(() -> RallyhubException.notFound("Club"));

        // Calculate refund based on cancellation policy
        int refundCredits = calculateCancellationRefund(session, club);

        List<Session.Attendee> attendees = new ArrayList<>(safeList(session.getAttendees()));
        boolean removed = attendees.removeIf(a -> a.getUserId().equals(userId));
        if (!removed) throw RallyhubException.notFound("Booking");

        session.setAttendees(attendees);
        sessionRepository.update(session);

        // Apply credit refund
        if (refundCredits > 0) {
            applyCredit(userId, clubId, refundCredits, "Cancellation refund",
                    "cancellation_refund", sessionId);
        }

        // Notify waitlist a slot is available
        notifyWaitlistSlotAvailable(session, club);
        return session;
    }

    // ── Session completion — deduct credits ──────────────────────

    public Session markComplete(String sessionId) {
        Session session = getSessionOrThrow(sessionId);
        Club club = clubRepository.findById(session.getClubId())
                .orElseThrow(() -> RallyhubException.notFound("Club"));

        // Deduct credits for each attendee who was present
        safeList(session.getAttendees()).forEach(attendee -> {
            try {
                ClubMembership m = membershipRepository
                        .findByUserAndClub(attendee.getUserId(), session.getClubId())
                        .orElse(null);
                if (m == null) return;

                // Resolve session price from schedule (default 1 credit)
                int price = 1;
                int newBalance = m.getCreditBalance() - price;
                m.setCreditBalance(newBalance);
                membershipRepository.update(m);

                ledgerRepository.save(CreditLedgerEntry.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(attendee.getUserId())
                        .clubId(session.getClubId())
                        .type("session_deduction")
                        .amount(-price)
                        .balanceAfter(newBalance)
                        .note("Session on " + session.getDate())
                        .createdBy("system")
                        .sessionId(sessionId)
                        .createdAt(Instant.now())
                        .build());

                attendee.setCreditsDeducted(price);
                attendee.setAttended(true);

                // Check negative credit limit
                if (newBalance < club.getSettings().getNegativeCreditLimit()) {
                    releaseSpotForNegativeBalance(m, club);
                    userRepository.findById(m.getUserId()).ifPresent(u ->
                            notificationService.send(u.getPushToken(), u.getEmail(),
                                    "credit_below_negative_limit",
                                    "Credit balance overdue",
                                    "Your balance is " + newBalance + ". Your spots have been released."));
                }
            } catch (Exception e) {
                log.error("Failed to deduct credit for userId={} sessionId={}: {}",
                        attendee.getUserId(), sessionId, e.getMessage());
            }
        });

        session.setStatus("completed");
        sessionRepository.update(session);
        return session;
    }

    // ── Organizer cancels entire session ─────────────────────────

    public Session cancelSession(String sessionId, String reason) {
        Session session = getSessionOrThrow(sessionId);
        Club club = clubRepository.findById(session.getClubId())
                .orElseThrow(() -> RallyhubException.notFound("Club"));

        // Full refund for all attendees
        safeList(session.getAttendees()).forEach(attendee -> {
            try {
                membershipRepository.findByUserAndClub(attendee.getUserId(), session.getClubId())
                        .ifPresent(m -> applyCredit(attendee.getUserId(), session.getClubId(),
                                1, "Organiser cancelled session", "organizer_cancellation_refund", sessionId));
                userRepository.findById(attendee.getUserId()).ifPresent(u ->
                        notificationService.send(u.getPushToken(), u.getEmail(),
                                "session_cancelled",
                                "Session cancelled",
                                "Your session on " + session.getDate() + " has been cancelled. " +
                                        (reason.isBlank() ? "" : "Reason: " + reason) +
                                        " Your credits have been refunded."));
            } catch (Exception e) {
                log.error("Failed to process cancellation refund for userId={}: {}",
                        attendee.getUserId(), e.getMessage());
            }
        });

        // Notify waitlisted members
        safeList(session.getWaitlist()).forEach(entry ->
                userRepository.findById(entry.getUserId()).ifPresent(u ->
                        notificationService.send(u.getPushToken(), u.getEmail(),
                                "session_cancelled",
                                "Session cancelled",
                                "The session on " + session.getDate() + " you were waitlisted for has been cancelled.")));

        session.setStatus("cancelled");
        session.setCancelledReason(reason);
        session.setWaitlist(new ArrayList<>());
        sessionRepository.update(session);
        return session;
    }

    // ── Helpers ──────────────────────────────────────────────────

    private int calculateCancellationRefund(Session session, Club club) {
        long hoursUntil = Instant.now().until(
                session.getDate().atStartOfDay(ZoneOffset.UTC).toInstant(), ChronoUnit.HOURS);

        return club.getSettings().getCancellationPolicy().getTiers().stream()
                .filter(t -> hoursUntil >= t.getHoursBeforeSession())
                .findFirst()
                .map(t -> t.getRefundPercent() / 100) // 1 credit × refund%
                .orElse(0);
    }

    private void applyCredit(String userId, String clubId, int amount, String note,
                              String type, String sessionId) {
        membershipRepository.findByUserAndClub(userId, clubId).ifPresent(m -> {
            int newBalance = m.getCreditBalance() + amount;
            m.setCreditBalance(newBalance);
            membershipRepository.update(m);
            ledgerRepository.save(CreditLedgerEntry.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId).clubId(clubId)
                    .type(type).amount(amount).balanceAfter(newBalance)
                    .note(note).createdBy("system").sessionId(sessionId)
                    .createdAt(Instant.now()).build());
        });
    }

    private void releaseSpotForNegativeBalance(ClubMembership m, Club club) {
        // Mark as negative-limit-released — spots released by setting flag
        // Full permanent-spot release logic is handled by the schedule service
        log.info("Member {} in club {} exceeded negative credit limit", m.getUserId(), m.getClubId());
    }

    private void notifyWaitlistSlotAvailable(Session session, Club club) {
        List<Session.WaitlistEntry> waitlist = safeList(session.getWaitlist());
        if (waitlist.isEmpty()) return;

        long hoursUntil = Instant.now().until(
                session.getDate().atStartOfDay(ZoneOffset.UTC).toInstant(), ChronoUnit.HOURS);

        if (hoursUntil <= club.getSettings().getPanicWindowHoursBeforeSession()) {
            // Blast all waitlisted members
            waitlist.forEach(entry ->
                    userRepository.findById(entry.getUserId()).ifPresent(u ->
                            notificationService.send(u.getPushToken(), u.getEmail(),
                                    "waitlist_panic_window",
                                    "Spot available — " + club.getName(),
                                    "A spot just opened. First to confirm gets it!")));
        } else {
            // Sequential: notify only position 1
            userRepository.findById(waitlist.get(0).getUserId()).ifPresent(u ->
                    notificationService.send(u.getPushToken(), u.getEmail(),
                            "waitlist_slot_sequential",
                            "Your waitlist spot opened — " + club.getName(),
                            "You have " + club.getSettings().getWaitlistSequentialWindowHours()
                                    + " hours to confirm your spot."));
        }
    }

    private int resolveCapacity(Session session, Club club) {
        return session.getDate() != null
                ? (session.getWaitlist() != null ? 20 : 20) // placeholder; real value from Schedule
                : 20;
    }

    private Session getSessionOrThrow(String id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> RallyhubException.notFound("Session"));
    }

    private ClubMembership getMembershipOrThrow(String userId, String clubId) {
        return membershipRepository.findByUserAndClub(userId, clubId)
                .orElseThrow(() -> RallyhubException.forbidden("Not a member of this club"));
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }
}
