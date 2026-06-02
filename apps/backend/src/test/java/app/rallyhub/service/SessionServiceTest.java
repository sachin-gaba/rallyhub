package app.rallyhub.service;

import app.rallyhub.domain.model.ClubMembership;
import app.rallyhub.domain.model.Session;
import app.rallyhub.domain.repository.*;
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

    @Mock SessionRepository      sessionRepo;
    @Mock MembershipRepository   membershipRepo;
    @Mock CreditLedgerRepository ledgerRepo;
    @Mock UserRepository         userRepo;
    @Mock ClubRepository         clubRepo;
    @Mock ScheduleService        scheduleService;
    @Mock NotificationService    notificationService;

    SessionService sessionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sessionService = new SessionService(
                sessionRepo, membershipRepo, ledgerRepo,
                userRepo, clubRepo, scheduleService, notificationService);
        // Default: capacity = 20
        when(scheduleService.resolveCapacity(any())).thenReturn(20);
    }

    // ── bookSession ──────────────────────────────────────────────

    @Test
    void bookSession_addsAttendee() {
        Session session = Session.builder().id("s1").clubId("c1").status("scheduled")
                .attendees(new ArrayList<>()).waitlist(new ArrayList<>()).build();
        ClubMembership m = ClubMembership.builder().userId("u1").clubId("c1").role("full_member").build();

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(membershipRepo.findByUserAndClub("u1", "c1")).thenReturn(Optional.of(m));

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
        ClubMembership m = ClubMembership.builder().userId("u1").clubId("c1").role("full_member").build();

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(membershipRepo.findByUserAndClub("u1", "c1")).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> sessionService.bookSession("u1", "c1", "s1"))
                .isInstanceOf(RallyhubException.class)
                .hasMessageContaining("Already booked");
    }

    @Test
    void bookSession_throwsForbiddenForInductee() {
        Session session = Session.builder().id("s1").clubId("c1").status("scheduled")
                .attendees(new ArrayList<>()).build();
        ClubMembership m = ClubMembership.builder().userId("u1").clubId("c1").role("inductee").build();

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(membershipRepo.findByUserAndClub("u1", "c1")).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> sessionService.bookSession("u1", "c1", "s1"))
                .isInstanceOf(RallyhubException.class)
                .hasMessageContaining("Inductees");
    }

    @Test
    void bookSession_throwsConflictWhenFull() {
        when(scheduleService.resolveCapacity(any())).thenReturn(1);
        Session.Attendee existing = Session.Attendee.builder().userId("u2").build();
        Session session = Session.builder().id("s1").clubId("c1").status("scheduled")
                .attendees(new ArrayList<>(java.util.List.of(existing))).build();
        ClubMembership m = ClubMembership.builder().userId("u1").clubId("c1").role("full_member").build();

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(membershipRepo.findByUserAndClub("u1", "c1")).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> sessionService.bookSession("u1", "c1", "s1"))
                .isInstanceOf(RallyhubException.class)
                .hasMessageContaining("full");
    }

    // ── joinWaitlist ─────────────────────────────────────────────

    @Test
    void joinWaitlist_addsEntryWithCorrectPosition() {
        Session session = Session.builder().id("s1").clubId("c1")
                .waitlist(new ArrayList<>()).attendees(new ArrayList<>()).build();
        ClubMembership m = ClubMembership.builder().userId("u1").clubId("c1").role("full_member").build();

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(membershipRepo.findByUserAndClub("u1", "c1")).thenReturn(Optional.of(m));

        sessionService.joinWaitlist("u1", "c1", "s1");

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepo).update(captor.capture());
        assertThat(captor.getValue().getWaitlist()).hasSize(1);
        assertThat(captor.getValue().getWaitlist().get(0).getPosition()).isEqualTo(1);
        assertThat(captor.getValue().getWaitlist().get(0).getUserId()).isEqualTo("u1");
    }

    @Test
    void joinWaitlist_throwsConflictIfAlreadyWaiting() {
        Session.WaitlistEntry existing = Session.WaitlistEntry.builder().userId("u1").position(1).build();
        Session session = Session.builder().id("s1").clubId("c1")
                .waitlist(new ArrayList<>(java.util.List.of(existing))).build();
        ClubMembership m = ClubMembership.builder().userId("u1").clubId("c1").role("full_member").build();

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(membershipRepo.findByUserAndClub("u1", "c1")).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> sessionService.joinWaitlist("u1", "c1", "s1"))
                .isInstanceOf(RallyhubException.class)
                .hasMessageContaining("waitlist");
    }

    // ── cancelBooking ────────────────────────────────────────────

    @Test
    void cancelBooking_removesAttendee() {
        Session.Attendee attendee = Session.Attendee.builder().userId("u1").build();
        Session session = Session.builder().id("s1").clubId("c1").date(java.time.LocalDate.now().plusDays(3))
                .attendees(new ArrayList<>(java.util.List.of(attendee)))
                .waitlist(new ArrayList<>()).build();
        app.rallyhub.domain.model.Club club = app.rallyhub.domain.model.Club.builder()
                .id("c1").settings(app.rallyhub.domain.model.Club.ClubSettings.builder()
                        .negativeCreditLimit(-2)
                        .panicWindowHoursBeforeSession(24)
                        .cancellationPolicy(app.rallyhub.domain.model.Club.CancellationPolicy.builder()
                                .tiers(java.util.List.of(
                                        new app.rallyhub.domain.model.Club.CancellationTier(48, 100),
                                        new app.rallyhub.domain.model.Club.CancellationTier(0, 0)))
                                .build())
                        .build())
                .build();

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(clubRepo.findById("c1")).thenReturn(Optional.of(club));
        when(membershipRepo.findByUserAndClub(any(), any())).thenReturn(Optional.empty());

        sessionService.cancelBooking("u1", "c1", "s1");

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepo).update(captor.capture());
        assertThat(captor.getValue().getAttendees()).isEmpty();
    }

    @Test
    void cancelBooking_throwsNotFoundIfNotBooked() {
        Session session = Session.builder().id("s1").clubId("c1")
                .date(java.time.LocalDate.now().plusDays(1))
                .attendees(new ArrayList<>()).waitlist(new ArrayList<>()).build();
        app.rallyhub.domain.model.Club club = app.rallyhub.domain.model.Club.builder()
                .id("c1").settings(app.rallyhub.domain.model.Club.ClubSettings.builder()
                        .cancellationPolicy(app.rallyhub.domain.model.Club.CancellationPolicy.builder()
                                .tiers(new ArrayList<>()).build())
                        .build())
                .build();

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(session));
        when(clubRepo.findById("c1")).thenReturn(Optional.of(club));

        assertThatThrownBy(() -> sessionService.cancelBooking("u1", "c1", "s1"))
                .isInstanceOf(RallyhubException.class)
                .hasMessageContaining("Booking");
    }
}
