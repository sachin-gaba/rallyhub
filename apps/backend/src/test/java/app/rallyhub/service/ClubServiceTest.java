package app.rallyhub.service;

import app.rallyhub.domain.model.Club;
import app.rallyhub.domain.model.ClubMembership;
import app.rallyhub.domain.repository.ClubRepository;
import app.rallyhub.domain.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClubServiceTest {

    @Mock ClubRepository clubRepo;
    @Mock MembershipRepository membershipRepo;

    ClubService clubService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(clubRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(membershipRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        clubService = new ClubService(clubRepo, membershipRepo);
    }

    @Test
    void createClub_setsOrganizerPrimaryRole() {
        Club club = clubService.createClub("user-1", "Test Club", List.of("badminton"), "Sports Hall");

        assertThat(club.getName()).isEqualTo("Test Club");
        assertThat(club.getInviteCode()).hasSize(6);
        assertThat(club.getPlan()).isEqualTo("starter");
        assertThat(club.getSettings().getNegativeCreditLimit()).isEqualTo(-2);

        ArgumentCaptor<ClubMembership> captor = ArgumentCaptor.forClass(ClubMembership.class);
        verify(membershipRepo).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("organizer_primary");
        assertThat(captor.getValue().getUserId()).isEqualTo("user-1");
    }

    @Test
    void createClub_defaultCancellationPolicyHasThreeTiers() {
        Club club = clubService.createClub("user-1", "Test", List.of("tennis"), "Venue");
        assertThat(club.getSettings().getCancellationPolicy().getTiers()).hasSize(3);
        assertThat(club.getSettings().getCancellationPolicy().getTiers().get(0).getRefundPercent()).isEqualTo(100);
    }
}
