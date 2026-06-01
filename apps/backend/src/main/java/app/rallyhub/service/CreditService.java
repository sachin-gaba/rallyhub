package app.rallyhub.service;

import app.rallyhub.domain.model.*;
import app.rallyhub.domain.repository.*;
import app.rallyhub.exception.RallyhubException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final MembershipRepository membershipRepository;
    private final CreditLedgerRepository ledgerRepository;

    private static final List<String> ORGANIZER_ROLES =
            List.of("organizer_primary", "organizer_additional", "co_organizer");

    public ClubMembership adjustCredit(String actorId, String clubId, String targetUserId,
                                       int amount, String note) {
        // Verify actor is organizer/co-organizer
        membershipRepository.findByUserAndClub(actorId, clubId)
                .filter(m -> ORGANIZER_ROLES.contains(m.getRole()))
                .orElseThrow(() -> RallyhubException.forbidden("Only organisers can adjust credits"));

        ClubMembership target = membershipRepository.findByUserAndClub(targetUserId, clubId)
                .orElseThrow(() -> RallyhubException.notFound("Member"));

        int newBalance = target.getCreditBalance() + amount;
        target.setCreditBalance(newBalance);
        membershipRepository.update(target);

        ledgerRepository.save(CreditLedgerEntry.builder()
                .id(UUID.randomUUID().toString())
                .userId(targetUserId)
                .clubId(clubId)
                .type("correction")
                .amount(amount)
                .balanceAfter(newBalance)
                .note(note)
                .createdBy(actorId)
                .createdAt(Instant.now())
                .build());

        return target;
    }

    public List<CreditLedgerEntry> getLedger(String userId, String clubId) {
        return ledgerRepository.findByUserAndClub(userId, clubId);
    }

    /**
     * Called when an organizer verifies a bank transfer.
     */
    public ClubMembership topUp(String actorId, String clubId, String userId, int creditsToAdd) {
        return adjustCredit(actorId, clubId, userId, creditsToAdd,
                "Top up verified by organiser");
    }
}
