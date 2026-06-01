package app.rallyhub.service;

import app.rallyhub.domain.model.ClubMembership;
import app.rallyhub.domain.model.HealthDeclaration;
import app.rallyhub.domain.repository.HealthDeclarationRepository;
import app.rallyhub.domain.repository.MembershipRepository;
import app.rallyhub.exception.RallyhubException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HealthDeclarationServiceTest {

    @Mock HealthDeclarationRepository declarationRepo;
    @Mock MembershipRepository membershipRepo;

    HealthDeclarationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(declarationRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new HealthDeclarationService(declarationRepo, membershipRepo);
    }

    private HealthDeclaration.EmergencyContact validEc() {
        return HealthDeclaration.EmergencyContact.builder()
                .fullName("Jane Doe").relationship("Spouse").primaryPhone("07700900000").build();
    }

    @Test
    void submit_throwsBadRequestWithoutLiabilityAccepted() {
        assertThatThrownBy(() -> service.submit("u1", "c1", Map.of(), List.of(), "", validEc(), false, true, "", ""))
                .isInstanceOf(RallyhubException.class)
                .hasMessageContaining("Liability waiver");
    }

    @Test
    void submit_throwsBadRequestWithoutDataConsent() {
        assertThatThrownBy(() -> service.submit("u1", "c1", Map.of(), List.of(), "", validEc(), true, false, "", ""))
                .isInstanceOf(RallyhubException.class)
                .hasMessageContaining("Liability waiver");
    }

    @Test
    void submit_savesDeclarationAndUpdatesMembership() {
        ClubMembership membership = ClubMembership.builder().userId("u1").clubId("c1").role("inductee").build();
        when(membershipRepo.findByUserAndClub("u1", "c1")).thenReturn(Optional.of(membership));

        var result = service.submit("u1", "c1", Map.of(0, false), List.of("None of the above"),
                "", validEc(), true, true, "iPhone", "1.0.0");

        assertThat(result.getUserId()).isEqualTo("u1");
        assertThat(result.isLiabilityAccepted()).isTrue();
        assertThat(membership.isHealthDeclarationSubmitted()).isTrue();
        verify(declarationRepo).save(any());
        verify(membershipRepo).update(membership);
    }

    @Test
    void promoteToFullMember_throwsIfDeclarationNotSubmitted() {
        ClubMembership actor  = ClubMembership.builder().userId("org1").clubId("c1").role("organizer_primary").build();
        ClubMembership target = ClubMembership.builder().userId("m1").clubId("c1").role("inductee")
                .healthDeclarationSubmitted(false).build();
        when(membershipRepo.findByUserAndClub("org1", "c1")).thenReturn(Optional.of(actor));
        when(membershipRepo.findByUserAndClub("m1",   "c1")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.promoteToFullMember("org1", "c1", "m1"))
                .isInstanceOf(RallyhubException.class)
                .hasMessageContaining("Health declaration not yet submitted");
    }
}
