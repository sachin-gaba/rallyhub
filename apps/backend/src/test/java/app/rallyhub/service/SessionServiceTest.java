package app.rallyhub.service;

import app.rallyhub.domain.model.ClubMembership;
import app.rallyhub.domain.model.Session;
import app.rallyhub.domain.repository.MembershipRepository;
import app.rallyhub.domain.repository.SessionRepository;
import app.rallyhub.exception.RallyhubException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionServiceTest {

    @Mock SessionRepository sessionRepo;
    @Mock MembershipRepository membershipRepo;

    SessionService sessionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sessionService = new SessionService(sessionRepo, membershipRepo);
    }

    @Test
    void bookSession_successfullyAddsAttendee() {
        Session session = Session.builder().id("s1").clubId("c1").status("scheduled")
                .attendees(new ArrayList<>()).waitlist(new ArrayList<>()).build();
        ClubMembership membership = ClubMembership.builder().userId("u1").clubId("c1")
                .role("full_member").build();

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(membershipRepo.findByUserAndClub("u1", "c1")).thenReturn(Optional.of(membership));

        sessionService.bookSession("u1", "c1", "s1");

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepo).update(captor.capture());
        assertThat(captor.getValue().getAttendees()).hasSize(1);
        assertThat(captor.getValue().getAttendees().get(0).getUserId()).isEqualTo("u1");
    }

    @Test
    void bookSession_throwsConflictIfAlreadyBooked() {
        Session.Attendee existing = Session.Attendee.builder().userId("u1").build();
        Session session = Session.builder().id("s1").clubId("c1").status("scheduled")
                .attendees(new ArrayList<>(java.util.List.of(existing))).build();
        ClubMembership membership = ClubMembership.builder().userId("u1").clubId("c1")
                .role("full_member").build();

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(membershipRepo.findByUserAndClub("u1", "c1")).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> sessionService.bookSession("u1", "c1", "s1"))
                .isInstanceOf(RallyhubException.class)
                .hasMessageContaining("Already booked");
    }

    @Test
    void bookSession_throwsForbiddenForInductee() {
        Session session = Session.builder().id("s1").clubId("c1").status("scheduled")
                .attendees(new ArrayList<>()).build();
        ClubMembership membership = ClubMembership.builder().userId("u1").clubId("c1")
                .role("inductee").build();

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(membershipRepo.findByUserAndClub("u1", "c1")).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> sessionService.bookSession("u1", "c1", "s1"))
                .isInstanceOf(RallyhubException.class)
                .hasMessageContaining("Inductees");
    }

    @Test
    void joinWaitlist_addsEntryWithCorrectPosition() {
        Session session = Session.builder().id("s1").clubId("c1")
                .waitlist(new ArrayList<>()).attendees(new ArrayList<>()).build();

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));

        sessionService.joinWaitlist("u1", "c1", "s1");

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepo).update(captor.capture());
        assertThat(captor.getValue().getWaitlist()).hasSize(1);
        assertThat(captor.getValue().getWaitlist().get(0).getPosition()).isEqualTo(1);
    }
}
