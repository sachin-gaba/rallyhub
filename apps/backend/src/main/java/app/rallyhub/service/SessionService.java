package app.rallyhub.service;

import app.rallyhub.domain.model.*;
import app.rallyhub.domain.repository.*;
import app.rallyhub.exception.RallyhubException;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Singleton
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MembershipRepository membershipRepository;
    private final CreditLedgerRepository ledgerRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final ScheduleService scheduleService;
    private final NotificationService notificationService;

    public SessionService(SessionRepository sessionRepository,
                          MembershipRepository membershipRepository,
                          CreditLedgerRepository ledgerRepository,
                          UserRepository userRepository,
                          ClubRepository clubRepository,
                          ScheduleService scheduleService,
                          NotificationService notificationService) {
        this.sessionRepository    = sessionRepository;
        this.membershipRepository = membershipRepository;
        this.ledgerRepository     = ledgerRepository;
        this.userRepository       = userRepository;
        this.clubRepository       = clubRepository;
        this.scheduleService      = scheduleService;
        this.notificationService  = notificationService;
    }

    // ── Booking ──────────────────────────────────────────────────

    public Session bookSession(String userId, String clubId, String sessionId) {
        Session session       = getSessionOrThrow(sessionId);
        ClubMembership member = getMemberOrThrow(userId, clubId);

        if ("inductee".equals(member.getRole()))
            throw RallyhubException.forbidden("Inductees may only book the induction session");

        List<Session.Attendee> attendees = mutable(session.getAttendees());
        if (attendees.stream().anyMatch(a -> a.getUserId().equals(userId)))
            throw RallyhubException.conflict("Already booked for this session");

        int capacity = scheduleService.resolveCapacity(session);
        if (attendees.size() >= capacity)
            throw RallyhubException.conflict("Session is full — join the waitlist");

        attendees.add(Session.Attendee.builder().userId(userId).bookedAt(Instant.now()).build());
        session.setAttendees(attendees);
        sessionRepository.update(session);
        return session;
    }

    public Session cancelBooking(String userId, String clubId, String sessionId) {
        Session session = getSessionOrThrow(sessionId);
        Club    club    = getClubOrThrow(clubId);

        int refundCredits = calculateCancellationRefund(session, club);

        List<Session.Attendee> attendees = mutable(session.getAttendees());
        if (!attendees.removeIf(a -> a.getUserId().equals(userId)))
            throw RallyhubException.notFound("Booking");

        session.setAttendees(attendees);
        sessionRepository.update(session);

        if (refundCredits > 0)
            applyCredit(userId, clubId, refundCredits, "Cancellation refund",
                    "cancellation_refund", sessionId);

        notifyWaitlistSlotAvailable(session, club);
        return session;
    }

    // ── Session completion — deduct credits for attendees ────────

    public Session markComplete(String sessionId) {
        Session session = getSessionOrThrow(sessionId);
        Club    club    = getClubOrThrow(session.getClubId());
        int     price   = resolveSessionPrice(session);

        mutable(session.getAttendees()).forEach(attendee -> {
            try {
                membershipRepository.findByUserAndClub(attendee.getUserId(), session.getClubId())
                        .ifPresent(m -> {
                            int newBalance = m.getCreditBalance() - price;
                            m.setCreditBalance(newBalance);
                            membershipRepository.update(m);

                            ledgerRepository.save(CreditLedgerEntry.builder()
                                    .id(UUID.randomUUID().toString())
                                    .userId(attendee.getUserId()).clubId(session.getClubId())
                                    .type("session_deduction").amount(-price).balanceAfter(newBalance)
                                    .note("Session " + session.getDate()).createdBy("system")
                                    .sessionId(sessionId).createdAt(Instant.now()).build());

                            attendee.setCreditsDeducted(price);
                            attendee.setAttended(true);

                            // Enforce negative credit limit
                            if (newBalance < club.getSettings().getNegativeCreditLimit()) {
                                log.info("Member {} exceeded negative limit in club {}", m.getUserId(), m.getClubId());
                                userRepository.findById(m.getUserId()).ifPresent(u ->
                                        notificationService.send(u.getPushToken(), u.getEmail(),
                                                "credit_below_negative_limit",
                                                "Credit balance overdue",
                                                "Your balance is " + newBalance + " credits. Please top up."));
                            }
                        });
            } catch (Exception ex) {
                log.error("Credit deduction failed userId={} session={}: {}", attendee.getUserId(), sessionId, ex.getMessage());
            }
        });

        session.setStatus("completed");
        sessionRepository.update(session);
        return session;
    }

    // ── Organizer cancels entire session ─────────────────────────

    public Session cancelSession(String sessionId, String reason) {
        Session session = getSessionOrThrow(sessionId);
        Club    club    = getClubOrThrow(session.getClubId());
        int     price   = resolveSessionPrice(session);

        mutable(session.getAttendees()).forEach(attendee -> {
            try {
                applyCredit(attendee.getUserId(), session.getClubId(), price,
                        "Organiser cancelled session", "organizer_cancellation_refund", sessionId);
                userRepository.findById(attendee.getUserId()).ifPresent(u ->
                        notificationService.send(u.getPushToken(), u.getEmail(),
                                "session_cancelled", "Session cancelled",
                                "Your " + session.getDate() + " session was cancelled"
                                        + (reason.isBlank() ? "." : ": " + reason)
                                        + " Credits refunded."));
            } catch (Exception ex) {
                log.error("Organiser cancel refund failed userId={}: {}", attendee.getUserId(), ex.getMessage());
            }
        });

        mutable(session.getWaitlist()).forEach(entry ->
                userRepository.findById(entry.getUserId()).ifPresent(u ->
                        notificationService.send(u.getPushToken(), u.getEmail(),
                                "session_cancelled", "Session cancelled",
                                "The session on " + session.getDate() + " you were waitlisted for was cancelled.")));

        session.setStatus("cancelled");
        session.setCancelledReason(reason);
        session.setWaitlist(new ArrayList<>());
        sessionRepository.update(session);
        return session;
    }

    public List<Session> listUpcoming(String clubId) {
        return sessionRepository.findByClubId(clubId).stream()
                .filter(s -> "scheduled".equals(s.getStatus()))
                .filter(s -> !s.getDate().isBefore(LocalDate.now(ZoneOffset.UTC)))
                .sorted(Comparator.comparing(Session::getDate))
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private int calculateCancellationRefund(Session session, Club club) {
        long hoursUntil = Instant.now().until(
                session.getDate().atStartOfDay(ZoneOffset.UTC).toInstant(), ChronoUnit.HOURS);
        int price = resolveSessionPrice(session);
        return club.getSettings().getCancellationPolicy().getTiers().stream()
                .filter(t -> hoursUntil >= t.getHoursBeforeSession())
                .findFirst()
                .map(t -> (int) Math.round(price * t.getRefundPercent() / 100.0))
                .orElse(0);
    }

    private int resolveSessionPrice(Session session) {
        if (session.getScheduleId() == null) return 1;
        try {
            return scheduleService.getSchedule(session.getScheduleId()).getPriceCredits();
        } catch (RallyhubException e) { return 1; }
    }

    private void applyCredit(String userId, String clubId, int amount, String note,
                              String type, String sessionId) {
        membershipRepository.findByUserAndClub(userId, clubId).ifPresent(m -> {
            int nb = m.getCreditBalance() + amount;
            m.setCreditBalance(nb);
            membershipRepository.update(m);
            ledgerRepository.save(CreditLedgerEntry.builder()
                    .id(UUID.randomUUID().toString()).userId(userId).clubId(clubId)
                    .type(type).amount(amount).balanceAfter(nb).note(note)
                    .createdBy("system").sessionId(sessionId).createdAt(Instant.now()).build());
        });
    }

    private void notifyWaitlistSlotAvailable(Session session, Club club) {
        List<Session.WaitlistEntry> wl = session.getWaitlist();
        if (wl == null || wl.isEmpty()) return;
        long hoursUntil = Instant.now().until(
                session.getDate().atStartOfDay(ZoneOffset.UTC).toInstant(), ChronoUnit.HOURS);
        if (hoursUntil <= club.getSettings().getPanicWindowHoursBeforeSession()) {
            wl.forEach(e -> userRepository.findById(e.getUserId()).ifPresent(u ->
                    notificationService.send(u.getPushToken(), u.getEmail(),
                            "waitlist_panic_window", "Spot open — " + club.getName(),
                            "A spot just opened — first to confirm gets it!")));
        } else {
            userRepository.findById(wl.get(0).getUserId()).ifPresent(u ->
                    notificationService.send(u.getPushToken(), u.getEmail(),
                            "waitlist_slot_sequential", "Your waitlist turn — " + club.getName(),
                            "You have " + club.getSettings().getWaitlistSequentialWindowHours()
                                    + "h to confirm your spot."));
        }
    }

    private Session  getSessionOrThrow(String id) { return sessionRepository.findById(id).orElseThrow(() -> RallyhubException.notFound("Session")); }
    private Club     getClubOrThrow(String id)    { return clubRepository.findById(id).orElseThrow(() -> RallyhubException.notFound("Club")); }
    private ClubMembership getMemberOrThrow(String uid, String cid) { return membershipRepository.findByUserAndClub(uid, cid).orElseThrow(() -> RallyhubException.forbidden("Not a member")); }
    private <T> List<T> mutable(List<T> l)        { return l == null ? new ArrayList<>() : new ArrayList<>(l); }
}
