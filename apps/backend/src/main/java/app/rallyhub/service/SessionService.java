package app.rallyhub.service;

import app.rallyhub.domain.model.*;
import app.rallyhub.domain.repository.*;
import app.rallyhub.exception.RallyhubException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MembershipRepository membershipRepository;

    public Session bookSession(String userId, String clubId, String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> RallyhubException.notFound("Session"));

        ClubMembership membership = membershipRepository.findByUserAndClub(userId, clubId)
                .orElseThrow(() -> RallyhubException.forbidden("Not a member of this club"));

        if ("inductee".equals(membership.getRole()))
            throw RallyhubException.forbidden("Inductees may only book induction sessions");

        List<Session.Attendee> attendees = session.getAttendees() == null
                ? new ArrayList<>() : new ArrayList<>(session.getAttendees());

        boolean alreadyBooked = attendees.stream().anyMatch(a -> a.getUserId().equals(userId));
        if (alreadyBooked) throw RallyhubException.conflict("Already booked for this session");

        // TODO: fetch schedule for capacity — for now use list size vs a reasonable default
        // Full capacity check will be added when Schedule service is wired
        attendees.add(Session.Attendee.builder().userId(userId).bookedAt(Instant.now()).build());
        session.setAttendees(attendees);
        sessionRepository.update(session);
        return session;
    }

    public Session joinWaitlist(String userId, String clubId, String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> RallyhubException.notFound("Session"));

        List<Session.WaitlistEntry> waitlist = session.getWaitlist() == null
                ? new ArrayList<>() : new ArrayList<>(session.getWaitlist());

        boolean alreadyWaiting = waitlist.stream().anyMatch(w -> w.getUserId().equals(userId));
        if (alreadyWaiting) throw RallyhubException.conflict("Already on waitlist");

        waitlist.add(Session.WaitlistEntry.builder()
                .userId(userId)
                .joinedAt(Instant.now())
                .position(waitlist.size() + 1)
                .build());

        session.setWaitlist(waitlist);
        sessionRepository.update(session);
        return session;
    }

    public Session cancelBooking(String userId, String clubId, String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> RallyhubException.notFound("Session"));

        List<Session.Attendee> attendees = session.getAttendees() == null
                ? new ArrayList<>() : new ArrayList<>(session.getAttendees());

        boolean removed = attendees.removeIf(a -> a.getUserId().equals(userId));
        if (!removed) throw RallyhubException.notFound("Booking");

        session.setAttendees(attendees);
        sessionRepository.update(session);

        // TODO: trigger credit refund based on cancellation policy + notify next waitlist member
        return session;
    }

    public Session markComplete(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> RallyhubException.notFound("Session"));
        session.setStatus("completed");
        sessionRepository.update(session);
        // TODO: deduct credits for all attendees
        return session;
    }

    public Session cancelSession(String sessionId, String reason) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> RallyhubException.notFound("Session"));
        session.setStatus("cancelled");
        session.setCancelledReason(reason);
        sessionRepository.update(session);
        // TODO: issue full credit refunds + notify all attendees + clear waitlist
        return session;
    }
}
