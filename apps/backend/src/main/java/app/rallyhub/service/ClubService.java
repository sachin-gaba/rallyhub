package app.rallyhub.service;

import app.rallyhub.domain.model.Club;
import app.rallyhub.domain.model.ClubMembership;
import app.rallyhub.domain.repository.ClubRepository;
import app.rallyhub.domain.repository.MembershipRepository;
import app.rallyhub.exception.RallyhubException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;

    public Club createClub(String organizerUserId, String name, List<String> sports, String primaryVenue) {
        String clubId     = UUID.randomUUID().toString();
        String inviteCode = generateInviteCode();

        Club club = Club.builder()
                .id(clubId)
                .name(name)
                .sports(sports)
                .primaryVenue(primaryVenue)
                .inviteCode(inviteCode)
                .plan("starter")
                .planExpiresAt(Instant.now().plus(90, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .settings(Club.ClubSettings.builder()
                        .negativeCreditLimit(-2)
                        .guestSponsorLimitPerMonth(2)
                        .waitlistSequentialWindowHours(2)
                        .panicWindowHoursBeforeSession(24)
                        .cancellationPolicy(Club.CancellationPolicy.builder()
                                .tiers(List.of(
                                        new Club.CancellationTier(48, 100),
                                        new Club.CancellationTier(24, 50),
                                        new Club.CancellationTier(0,  0)))
                                .build())
                        .build())
                .build();

        clubRepository.save(club);

        // Creator becomes Primary Organizer
        ClubMembership membership = ClubMembership.builder()
                .userId(organizerUserId)
                .clubId(clubId)
                .role("organizer_primary")
                .creditBalance(0)
                .paymentReference(inviteCode + "-" + organizerUserId.substring(0, 4).toUpperCase())
                .joinedAt(Instant.now())
                .inductionCompleted(true)
                .healthDeclarationSubmitted(false)
                .build();

        membershipRepository.save(membership);
        return club;
    }

    public Club getClub(String clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> RallyhubException.notFound("Club"));
    }

    public Club getClubByInviteCode(String inviteCode) {
        return clubRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> RallyhubException.notFound("Club"));
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
