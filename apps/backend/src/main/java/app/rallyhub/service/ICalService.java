package app.rallyhub.service;

import app.rallyhub.domain.model.Session;
import app.rallyhub.domain.model.User;
import app.rallyhub.domain.repository.MembershipRepository;
import app.rallyhub.domain.repository.SessionRepository;
import app.rallyhub.domain.repository.UserRepository;
import app.rallyhub.exception.RallyhubException;
import jakarta.inject.Singleton;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates an authenticated iCal (.ics) feed of a member's booked sessions.
 * Spec section 10.5: per-user secret token, auto-updates when sessions change.
 */
@Singleton
public class ICalService {

    private static final DateTimeFormatter ICAL_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final SessionRepository sessionRepository;

    public ICalService(UserRepository userRepository,
                       MembershipRepository membershipRepository,
                       SessionRepository sessionRepository) {
        this.userRepository       = userRepository;
        this.membershipRepository = membershipRepository;
        this.sessionRepository    = sessionRepository;
    }

    /**
     * Resolve user from iCal token and generate .ics content.
     * URL: /v1/ical/{token}
     */
    public String generateFeed(String icalToken) {
        // Find user by icalToken (requires GSI on users table — icalToken-index)
        User user = userRepository.findByIcalToken(icalToken)
                .orElseThrow(() -> RallyhubException.notFound("iCal feed"));

        List<Session> bookedSessions = new ArrayList<>();
        membershipRepository.findByUserId(user.getId()).forEach(membership ->
                sessionRepository.findByClubId(membership.getClubId()).stream()
                        .filter(s -> "scheduled".equals(s.getStatus()) || "completed".equals(s.getStatus()))
                        .filter(s -> s.getAttendees() != null &&
                                s.getAttendees().stream().anyMatch(a -> a.getUserId().equals(user.getId())))
                        .forEach(bookedSessions::add));

        return buildIcs(user, bookedSessions);
    }

    /**
     * Generate or rotate a user's iCal token.
     */
    public String rotateIcalToken(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> RallyhubException.notFound("User"));
        String newToken = UUID.randomUUID().toString().replace("-", "");
        user.setIcalToken(newToken);
        userRepository.update(user);
        return newToken;
    }

    private String buildIcs(User user, List<Session> sessions) {
        StringBuilder sb = new StringBuilder();
        String now = ICAL_DATE.format(Instant.now());

        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//RallyHub//RallyHub Calendar//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("X-WR-CALNAME:RallyHub — ").append(user.getDisplayName()).append("\r\n");
        sb.append("X-WR-TIMEZONE:UTC\r\n");

        for (Session s : sessions) {
            LocalDate date      = s.getDate();
            // Sessions are all-day events (start time from Schedule would refine this)
            String dtStart = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String dtEnd   = date.plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:").append(s.getId()).append("@rallyhub.app\r\n");
            sb.append("DTSTAMP:").append(now).append("\r\n");
            sb.append("DTSTART;VALUE=DATE:").append(dtStart).append("\r\n");
            sb.append("DTEND;VALUE=DATE:").append(dtEnd).append("\r\n");
            sb.append("SUMMARY:RallyHub Session\r\n");
            sb.append("STATUS:").append("cancelled".equals(s.getStatus()) ? "CANCELLED" : "CONFIRMED").append("\r\n");
            if (s.getCancelledReason() != null)
                sb.append("DESCRIPTION:Cancelled — ").append(s.getCancelledReason()).append("\r\n");
            sb.append("END:VEVENT\r\n");
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }
}
