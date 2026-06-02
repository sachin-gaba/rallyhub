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
public class Club {

    @Getter(onMethod_ = {@DynamoDbPartitionKey})
    private String id;

    private String name;
    private List<String> sports;
    private String primaryVenue;
    private String inviteCode;
    private String welcomeMessage;
    private String rulesDocument;
    private String bankTransferInstructions;
    private String plan;           // starter | club | multi_club
    private Instant planExpiresAt;
    private Instant createdAt;
    private ClubSettings settings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Introspected
@DynamoDbBean
    public static class ClubSettings {
        private int negativeCreditLimit;              // default -2
        private int guestSponsorLimitPerMonth;        // default 2
        private int waitlistSequentialWindowHours;   // default 2
        private int panicWindowHoursBeforeSession;   // default 24
        private CancellationPolicy cancellationPolicy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Introspected
@DynamoDbBean
    public static class CancellationPolicy {
        private List<CancellationTier> tiers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Introspected
@DynamoDbBean
    public static class CancellationTier {
        private int hoursBeforeSession;
        private int refundPercent;  // 0–100
    }
}
