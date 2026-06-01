package app.rallyhub.service;

import app.rallyhub.domain.model.ClubMembership;
import app.rallyhub.domain.model.CreditLedgerEntry;
import app.rallyhub.domain.repository.CreditLedgerRepository;
import app.rallyhub.domain.repository.MembershipRepository;
import app.rallyhub.exception.RallyhubException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreditServiceTest {

    @Mock MembershipRepository membershipRepo;
    @Mock CreditLedgerRepository ledgerRepo;

    CreditService creditService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(ledgerRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        creditService = new CreditService(membershipRepo, ledgerRepo);
    }

    @Test
    void adjustCredit_updatesBalanceAndWritesLedger() {
        ClubMembership actor  = ClubMembership.builder().userId("org1").clubId("c1").role("organizer_primary").build();
        ClubMembership target = ClubMembership.builder().userId("m1").clubId("c1").creditBalance(5).build();

        when(membershipRepo.findByUserAndClub("org1", "c1")).thenReturn(Optional.of(actor));
        when(membershipRepo.findByUserAndClub("m1",   "c1")).thenReturn(Optional.of(target));

        creditService.adjustCredit("org1", "c1", "m1", 10, "Top up");

        assertThat(target.getCreditBalance()).isEqualTo(15);
        verify(membershipRepo).update(target);

        ArgumentCaptor<CreditLedgerEntry> captor = ArgumentCaptor.forClass(CreditLedgerEntry.class);
        verify(ledgerRepo).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(10);
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(15);
        assertThat(captor.getValue().getType()).isEqualTo("correction");
    }

    @Test
    void adjustCredit_throwsForbiddenForNonOrganizer() {
        ClubMembership actor = ClubMembership.builder().userId("m1").clubId("c1").role("full_member").build();
        when(membershipRepo.findByUserAndClub("m1", "c1")).thenReturn(Optional.of(actor));

        assertThatThrownBy(() -> creditService.adjustCredit("m1", "c1", "m2", 5, ""))
                .isInstanceOf(RallyhubException.class)
                .hasMessageContaining("organisers");
    }
}
