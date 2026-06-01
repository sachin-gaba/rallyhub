package app.rallyhub.service;

import app.rallyhub.domain.model.*;
import app.rallyhub.domain.repository.*;
import app.rallyhub.exception.RallyhubException;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final MembershipRepository membershipRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private static final List<String> DEFAULT_TIEBREAKERS =
            List.of("wins", "head_to_head", "points_difference", "total_points", "coin_flip");

    public TournamentService(TournamentRepository tournamentRepository,
                             MembershipRepository membershipRepository,
                             NotificationService notificationService,
                             UserRepository userRepository) {
        this.tournamentRepository = tournamentRepository;
        this.membershipRepository = membershipRepository;
        this.notificationService  = notificationService;
        this.userRepository       = userRepository;
    }

    // ── Create ───────────────────────────────────────────────────

    public Tournament createTournament(String actorId, String clubId, String name,
                                       String format, String drawType, String sport,
                                       List<String> participantIds, int groupSize) {
        membershipRepository.findByUserAndClub(actorId, clubId)
                .filter(m -> m.getRole().startsWith("organizer") || "co_organizer".equals(m.getRole()))
                .orElseThrow(() -> RallyhubException.forbidden("Only organisers can create tournaments"));

        List<Tournament.Participant> participants = participantIds.stream()
                .map(uid -> membershipRepository.findByUserAndClub(uid, clubId)
                        .map(m -> Tournament.Participant.builder()
                                .userId(uid)
                                .skillTier(m.getSkillLevel() != null ? m.getSkillLevel() : "intermediate")
                                .build())
                        .orElseThrow(() -> RallyhubException.notFound("Member " + uid)))
                .collect(Collectors.toList());

        List<Tournament.TournamentGroup> groups = buildGroups(participants, drawType, groupSize);
        List<Tournament.Match>           matches = generateGroupMatches(groups);

        Tournament t = Tournament.builder()
                .id(UUID.randomUUID().toString())
                .clubId(clubId).name(name).format(format).drawType(drawType).sport(sport)
                .participants(participants).groups(groups).matches(matches)
                .tiebreakerOrder(DEFAULT_TIEBREAKERS).createdAt(Instant.now())
                .build();
        return tournamentRepository.save(t);
    }

    // ── Score submission ─────────────────────────────────────────

    public Tournament submitScore(String actorId, String clubId, String tournamentId,
                                  String matchId, int score1, int score2) {
        Tournament t = getOrThrow(tournamentId);
        Tournament.Match match = findMatch(t, matchId);

        if (!match.getPlayer1Id().equals(actorId) && !match.getPlayer2Id().equals(actorId))
            throw RallyhubException.forbidden("Only match participants can submit scores");
        if ("confirmed".equals(match.getStatus()) || "resolved".equals(match.getStatus()))
            throw RallyhubException.conflict("Score already finalised");

        match.setScorePlayer1(score1);
        match.setScorePlayer2(score2);
        match.setSubmittedBy(actorId);
        match.setStatus("score_submitted");
        match.setCompletedAt(Instant.now());

        // Notify opponent
        String opponentId = match.getPlayer1Id().equals(actorId) ? match.getPlayer2Id() : match.getPlayer1Id();
        userRepository.findById(opponentId).ifPresent(u ->
                notificationService.send(u.getPushToken(), u.getEmail(),
                        "match_result_submitted", "Match score submitted",
                        "Your opponent submitted a score. Check and confirm."));

        tournamentRepository.update(t);
        return t;
    }

    // ── Score dispute ────────────────────────────────────────────

    public Tournament disputeScore(String actorId, String tournamentId, String matchId, String note) {
        Tournament t = getOrThrow(tournamentId);
        Tournament.Match match = findMatch(t, matchId);

        if (!"score_submitted".equals(match.getStatus()))
            throw RallyhubException.conflict("No submitted score to dispute");

        match.setStatus("disputed");
        match.setDisputeNote(note);
        tournamentRepository.update(t);

        // Notify organizer
        membershipRepository.findByClubId(t.getClubId()).stream()
                .filter(m -> m.getRole().startsWith("organizer") || "co_organizer".equals(m.getRole()))
                .forEach(m -> userRepository.findById(m.getUserId()).ifPresent(u ->
                        notificationService.send(u.getPushToken(), u.getEmail(),
                                "match_result_disputed", "Match score disputed",
                                "A match score has been disputed. Please review and resolve.")));
        return t;
    }

    // ── Organizer resolves dispute ────────────────────────────────

    public Tournament resolveDispute(String actorId, String clubId, String tournamentId,
                                     String matchId, int score1, int score2) {
        membershipRepository.findByUserAndClub(actorId, clubId)
                .filter(m -> m.getRole().startsWith("organizer") || "co_organizer".equals(m.getRole()))
                .orElseThrow(() -> RallyhubException.forbidden("Only organisers can resolve disputes"));

        Tournament t = getOrThrow(tournamentId);
        Tournament.Match match = findMatch(t, matchId);

        if (!"disputed".equals(match.getStatus()))
            throw RallyhubException.conflict("Match is not in disputed state");

        match.setScorePlayer1(score1);
        match.setScorePlayer2(score2);
        match.setStatus("resolved");
        match.setResolvedBy(actorId);
        tournamentRepository.update(t);

        // Notify both players
        List.of(match.getPlayer1Id(), match.getPlayer2Id()).forEach(uid ->
                userRepository.findById(uid).ifPresent(u ->
                        notificationService.send(u.getPushToken(), u.getEmail(),
                                "match_result_disputed", "Dispute resolved",
                                "The organiser has resolved the match score.")));
        return t;
    }

    // ── Standings / tie-breaker calculation ──────────────────────

    public List<Map<String, Object>> calculateGroupStandings(String tournamentId, String groupId) {
        Tournament t = getOrThrow(tournamentId);
        Tournament.TournamentGroup group = t.getGroups().stream()
                .filter(g -> g.getId().equals(groupId))
                .findFirst().orElseThrow(() -> RallyhubException.notFound("Group"));

        List<String> playerIds = group.getParticipantIds();
        Map<String, PlayerStats> stats = new LinkedHashMap<>();
        playerIds.forEach(pid -> stats.put(pid, new PlayerStats(pid)));

        // Accumulate stats from confirmed/resolved matches within this group
        t.getMatches().stream()
                .filter(m -> isGroupMatch(m, playerIds))
                .filter(m -> "confirmed".equals(m.getStatus()) || "resolved".equals(m.getStatus()))
                .forEach(m -> {
                    if (m.getScorePlayer1() == null || m.getScorePlayer2() == null) return;
                    PlayerStats s1 = stats.get(m.getPlayer1Id());
                    PlayerStats s2 = stats.get(m.getPlayer2Id());
                    if (s1 == null || s2 == null) return;

                    s1.played++; s2.played++;
                    s1.pointsFor += m.getScorePlayer1(); s1.pointsAgainst += m.getScorePlayer2();
                    s2.pointsFor += m.getScorePlayer2(); s2.pointsAgainst += m.getScorePlayer1();

                    if (m.getScorePlayer1() > m.getScorePlayer2()) {
                        s1.wins++;
                        s2.losses++;
                        s1.headToHead.merge(m.getPlayer2Id(), 1, Integer::sum);
                    } else if (m.getScorePlayer2() > m.getScorePlayer1()) {
                        s2.wins++;
                        s1.losses++;
                        s2.headToHead.merge(m.getPlayer1Id(), 1, Integer::sum);
                    } else {
                        s1.draws++; s2.draws++;
                    }
                });

        // Sort using configured tiebreaker order
        List<PlayerStats> ranked = new ArrayList<>(stats.values());
        ranked.sort((a, b) -> compareByTiebreakers(a, b, t.getTiebreakerOrder()));

        return ranked.stream().map(PlayerStats::toMap).collect(Collectors.toList());
    }

    // ── Group generation helpers ──────────────────────────────────

    private List<Tournament.TournamentGroup> buildGroups(List<Tournament.Participant> participants,
                                                          String drawType, int groupSize) {
        List<Tournament.Participant> ordered = new ArrayList<>(participants);
        switch (drawType) {
            case "skill_matched" -> ordered.sort(Comparator.comparing(Tournament.Participant::getSkillTier));
            case "mixed_ability" -> {
                // Interleave: sort by skill, then distribute evenly across groups
                ordered.sort(Comparator.comparing(Tournament.Participant::getSkillTier));
                ordered = interleave(ordered, (int) Math.ceil((double) ordered.size() / groupSize));
            }
            default -> Collections.shuffle(ordered);
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

    /** Generate round-robin matches within each group. */
    private List<Tournament.Match> generateGroupMatches(List<Tournament.TournamentGroup> groups) {
        List<Tournament.Match> matches = new ArrayList<>();
        for (var group : groups) {
            List<String> pids = group.getParticipantIds();
            for (int i = 0; i < pids.size(); i++) {
                for (int j = i + 1; j < pids.size(); j++) {
                    matches.add(Tournament.Match.builder()
                            .id(UUID.randomUUID().toString())
                            .round(group.getName().toLowerCase().replace(" ", "_"))
                            .player1Id(pids.get(i)).player2Id(pids.get(j))
                            .status("scheduled").build());
                }
            }
        }
        return matches;
    }

    private int compareByTiebreakers(PlayerStats a, PlayerStats b, List<String> tiebreakers) {
        for (String rule : tiebreakers) {
            int cmp = switch (rule) {
                case "wins"              -> Integer.compare(b.wins, a.wins);
                case "head_to_head"      -> Integer.compare(b.headToHead.getOrDefault(a.playerId, 0), a.headToHead.getOrDefault(b.playerId, 0));
                case "points_difference" -> Integer.compare((b.pointsFor - b.pointsAgainst), (a.pointsFor - a.pointsAgainst));
                case "total_points"      -> Integer.compare(b.pointsFor, a.pointsFor);
                case "coin_flip"         -> new Random().nextBoolean() ? 1 : -1;
                default                  -> 0;
            };
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    private boolean isGroupMatch(Tournament.Match m, List<String> playerIds) {
        return playerIds.contains(m.getPlayer1Id()) && playerIds.contains(m.getPlayer2Id());
    }

    private List<Tournament.Participant> interleave(List<Tournament.Participant> sorted, int numGroups) {
        List<Tournament.Participant> result = new ArrayList<>();
        for (int g = 0; g < numGroups; g++)
            for (int i = g; i < sorted.size(); i += numGroups)
                result.add(sorted.get(i));
        return result;
    }

    private Tournament getOrThrow(String id) {
        return tournamentRepository.findById(id).orElseThrow(() -> RallyhubException.notFound("Tournament"));
    }

    private Tournament.Match findMatch(Tournament t, String matchId) {
        return t.getMatches().stream().filter(m -> m.getId().equals(matchId))
                .findFirst().orElseThrow(() -> RallyhubException.notFound("Match"));
    }

    // ── Inner stats accumulator ───────────────────────────────────

    private static class PlayerStats {
        final String playerId;
        int played, wins, losses, draws, pointsFor, pointsAgainst;
        final Map<String, Integer> headToHead = new HashMap<>();

        PlayerStats(String playerId) { this.playerId = playerId; }

        Map<String, Object> toMap() {
            return Map.of(
                "playerId", playerId,
                "played",   played,
                "wins",     wins,
                "losses",   losses,
                "draws",    draws,
                "pointsDiff", pointsFor - pointsAgainst,
                "pointsFor",  pointsFor
            );
        }
    }
}
