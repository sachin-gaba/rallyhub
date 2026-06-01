import jakarta.inject.Singleton;
package app.rallyhub.service;

import app.rallyhub.domain.model.*;
import app.rallyhub.domain.repository.*;
import app.rallyhub.exception.RallyhubException;
import lombok.RequiredArgsConstructor;


import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final MembershipRepository membershipRepository;

    private static final List<String> DEFAULT_TIEBREAKERS =
            List.of("wins", "head_to_head", "points_difference", "total_points", "coin_flip");

    public Tournament createTournament(String actorId, String clubId, String name,
                                       String format, String drawType, String sport,
                                       List<String> participantIds, int groupSize) {
        // Verify actor is organizer
        membershipRepository.findByUserAndClub(actorId, clubId)
                .filter(m -> m.getRole().startsWith("organizer") || "co_organizer".equals(m.getRole()))
                .orElseThrow(() -> RallyhubException.forbidden("Only organisers can create tournaments"));

        // Fetch skill tiers for all participants
        List<Tournament.Participant> participants = participantIds.stream()
                .map(uid -> membershipRepository.findByUserAndClub(uid, clubId)
                        .map(m -> Tournament.Participant.builder()
                                .userId(uid)
                                .skillTier(m.getSkillLevel() != null ? m.getSkillLevel() : "intermediate")
                                .build())
                        .orElseThrow(() -> RallyhubException.notFound("Member " + uid)))
                .collect(Collectors.toList());

        List<Tournament.TournamentGroup> groups = buildGroups(participants, drawType, groupSize);

        Tournament t = Tournament.builder()
                .id(UUID.randomUUID().toString())
                .clubId(clubId)
                .name(name)
                .format(format)
                .drawType(drawType)
                .sport(sport)
                .participants(participants)
                .groups(groups)
                .matches(new ArrayList<>())
                .tiebreakerOrder(DEFAULT_TIEBREAKERS)
                .createdAt(Instant.now())
                .build();

        return tournamentRepository.save(t);
    }

    public Tournament submitScore(String actorId, String clubId, String tournamentId,
                                  String matchId, int score1, int score2) {
        Tournament t = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> RallyhubException.notFound("Tournament"));

        Tournament.Match match = t.getMatches().stream()
                .filter(m -> m.getId().equals(matchId))
                .findFirst()
                .orElseThrow(() -> RallyhubException.notFound("Match"));

        if (!match.getPlayer1Id().equals(actorId) && !match.getPlayer2Id().equals(actorId))
            throw RallyhubException.forbidden("Only match participants can submit scores");

        match.setScorePlayer1(score1);
        match.setScorePlayer2(score2);
        match.setSubmittedBy(actorId);
        match.setStatus("score_submitted");

        tournamentRepository.update(t);
        return t;
    }

    public Tournament disputeScore(String actorId, String tournamentId, String matchId, String note) {
        Tournament t = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> RallyhubException.notFound("Tournament"));

        Tournament.Match match = t.getMatches().stream()
                .filter(m -> m.getId().equals(matchId))
                .findFirst()
                .orElseThrow(() -> RallyhubException.notFound("Match"));

        match.setStatus("disputed");
        match.setDisputeNote(note);
        tournamentRepository.update(t);
        // TODO: notify organizer
        return t;
    }

    private List<Tournament.TournamentGroup> buildGroups(List<Tournament.Participant> participants,
                                                          String drawType, int groupSize) {
        List<Tournament.Participant> ordered = new ArrayList<>(participants);

        if ("skill_matched".equals(drawType)) {
            ordered.sort(Comparator.comparing(Tournament.Participant::getSkillTier));
        } else if ("mixed_ability".equals(drawType)) {
            // interleave skill tiers for balanced groups
            ordered.sort(Comparator.comparing(Tournament.Participant::getSkillTier));
        } else {
            Collections.shuffle(ordered); // random
        }

        List<Tournament.TournamentGroup> groups = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i += groupSize) {
            List<Tournament.Participant> slice = ordered.subList(i, Math.min(i + groupSize, ordered.size()));
            groups.add(Tournament.TournamentGroup.builder()
                    .id(UUID.randomUUID().toString())
                    .name("Group " + (char)('A' + groups.size()))
                    .participantIds(slice.stream().map(Tournament.Participant::getUserId).collect(Collectors.toList()))
                    .build());
        }
        return groups;
    }
}
