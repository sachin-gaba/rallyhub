package app.rallyhub.domain.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class ClubMembership {

    @DynamoDbPartitionKey
    private String userId;

    @DynamoDbSortKey
    private String clubId;

    /** organizer_primary | organizer_additional | co_organizer | full_member | inductee */
    private String role;

    private int creditBalance;
    private String paymentReference;
    private String skillLevel;   // beginner | improver | intermediate | advanced | elite
    private Instant joinedAt;
    private boolean inductionCompleted;
    private boolean healthDeclarationSubmitted;
    private String healthDeclarationId;
}
