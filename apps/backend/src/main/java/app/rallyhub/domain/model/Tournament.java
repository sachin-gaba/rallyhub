package app.rallyhub.domain.model;
import io.micronaut.core.annotation.Introspected;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Introspected
@DynamoDbBean
public class Tournament {

    @Getter(onMethod_ = {@DynamoDbPartitionKey})
    private String id;

    private String clubId;
    private String name;
    private String format;   // group_knockout | round_robin
    private String drawType; // random | skill_matched | mixed_ability
    private String sport;
    private List<Participant> participants;
    private List<TournamentGroup> groups;
    private List<Match> matches;
    private List<String> tiebreakerOrder;
    private Instant createdAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor @Introspected
@DynamoDbBean
    public static class Participant {
        private String userId;
        private String skillTier;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor @Introspected
@DynamoDbBean
    public static class TournamentGroup {
        private String id;
        private String name;
        private List<String> participantIds;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor @Introspected
@DynamoDbBean
    public static class Match {
        private String id;
        private String round;      // e.g. group_a, qf, sf, final
        private String player1Id;
        private String player2Id;
        private Integer scorePlayer1;
        private Integer scorePlayer2;
        private String submittedBy;
        /** scheduled | score_submitted | disputed | confirmed | resolved */
        private String status;
        private String disputeNote;
        private String resolvedBy;
        private Instant scheduledAt;
        private Instant completedAt;
    }
}
