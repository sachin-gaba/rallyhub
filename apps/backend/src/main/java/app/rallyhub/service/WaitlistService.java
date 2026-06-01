import jakarta.inject.Singleton;
package app.rallyhub.service;

import app.rallyhub.domain.model.*;
import app.rallyhub.domain.repository.*;
import app.rallyhub.exception.RallyhubException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class WaitlistService {

    private final SessionRepository sessionRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final NotificationService notificationService;

    /**
     * Called when a slot opens (cancellation, bump, release).
     * Checks whether we are inside the panic window — if so, blast all waitlisted
     * members simultaneously. Otherwise open a sequential window for the next person.
     */
    public void onSlotAvailable(String sessionId, String clubId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> RallyhubException.notFound("Session"));
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> RallyhubException.notFound("Club"));

        List<Session.WaitlistEntry> waitlist = session.getWaitlist();
        if (waitlist == null || waitlist.isEmpty()) return;

        long hoursUntilSession = Instant.now()
                .until(sessionDateToInstant(session), ChronoUnit.HOURS);
        int panicHours = club.getSettings().getPanicWindowHoursBeforeSession();
        int seqHours   = club.getSettings().getWaitlistSequentialWindowHours();

        if (hoursUntilSession <= panicHours) {
            // Panic window: blast all
            notifyPanicWindow(session, club, waitlist);
        } else {
            // Sequential: notify first person
            notifySequential(session, club, waitlist.get(0), seqHours);
        }
    }

    /**
     * Called by a scheduled Lambda when a sequential window expires without response.
     */
    public void onSequentialWindowExpired(String sessionId, String userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> RallyhubException.notFound("Session"));

        List<Session.WaitlistEntry> waitlist = new ArrayList<>(session.getWaitlist());
        // Remove the timed-out entry and re-number positions
        waitlist.removeIf(w -> w.getUserId().equals(userId)
                && w.getWindowExpiresAt() != null
                && Instant.now().isAfter(w.getWindowExpiresAt()));

        for (int i = 0; i < waitlist.size(); i++) waitlist.get(i).setPosition(i + 1);
        session.setWaitlist(waitlist);
        sessionRepository.update(session);

        // Notify next person if still within sequential window period
        if (!waitlist.isEmpty()) {
            Club club = clubRepository.findById(session.getClubId()).orElseThrow();
            notifySequential(session, club, waitlist.get(0), club.getSettings().getWaitlistSequentialWindowHours());
        }
    }

    /** Member confirms their slot from a waitlist notification. */
    public Session confirmSlot(String userId, String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> RallyhubException.notFound("Session"));

        List<Session.WaitlistEntry> waitlist = new ArrayList<>(session.getWaitlist());
        boolean onWaitlist = waitlist.stream().anyMatch(w -> w.getUserId().equals(userId));
        if (!onWaitlist) throw RallyhubException.forbidden("Not on waitlist for this session");

        // Add to attendees
        List<Session.Attendee> attendees = session.getAttendees() == null
                ? new ArrayList<>() : new ArrayList<>(session.getAttendees());
        attendees.add(Session.Attendee.builder().userId(userId).bookedAt(Instant.now()).build());
        session.setAttendees(attendees);

        // Remove from waitlist and re-number
        waitlist.removeIf(w -> w.getUserId().equals(userId));
        for (int i = 0; i < waitlist.size(); i++) waitlist.get(i).setPosition(i + 1);
        session.setWaitlist(waitlist);

        sessionRepository.update(session);
        return session;
    }

    private void notifySequential(Session session, Club club,
                                   Session.WaitlistEntry entry, int windowHours) {
        Instant windowExpiry = Instant.now().plus(windowHours, ChronoUnit.HOURS);
        entry.setNotifiedAt(Instant.now());
        entry.setWindowExpiresAt(windowExpiry);
        sessionRepository.update(session);

        userRepository.findById(entry.getUserId()).ifPresent(user ->
                notificationService.send(
                        user.getPushToken(), user.getEmail(),
                        "waitlist_slot_sequential",
                        "Spot available — " + club.getName(),
                        "A spot opened in your session. Confirm within " + windowHours + " hours!")
        );
    }

    private void notifyPanicWindow(Session session, Club club,
                                    List<Session.WaitlistEntry> waitlist) {
        waitlist.forEach(entry ->
                userRepository.findById(entry.getUserId()).ifPresent(user ->
                        notificationService.send(
                                user.getPushToken(), user.getEmail(),
                                "waitlist_panic_window",
                                "Last chance — " + club.getName(),
                                "A spot just opened. First to confirm gets it!")
                ));
    }

    private Instant sessionDateToInstant(Session session) {
        // LocalDate at 00:00 UTC — fine for ordering purposes
        return session.getDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
    }
}
