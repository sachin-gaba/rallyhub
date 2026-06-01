import io.micronaut.core.annotation.Introspected;
package app.rallyhub.domain.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Introspected
@DynamoDbBean
public class Session {

    @DynamoDbPartitionKey
    private String id;

    private String scheduleId;
    private String clubId;
    private LocalDate date;

    /** scheduled | in_progress | completed | cancelled */
    private String status;

    private List<Attendee> attendees;
    private List<WaitlistEntry> waitlist;
    private List<GuestLedgerEntry> guestLedger;
    private String cancelledReason;
    private Instant createdAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor @Introspected
@DynamoDbBean
    public static class Attendee {
        private String userId;
        private Instant bookedAt;
        private Boolean attended;
        private Integer creditsDeducted;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor @Introspected
@DynamoDbBean
    public static class WaitlistEntry {
        private String userId;
        private Instant joinedAt;
        private int position;
        private Instant notifiedAt;
        private Instant windowExpiresAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor @Introspected
@DynamoDbBean
    public static class GuestLedgerEntry {
        private String id;
        private String guestName;
        private String sponsorUserId;
        private int sessionFee;
        /** cash_collected | bank_transfer_pending | complimentary */
        private String paymentMethod;
        private String note;
        private Instant createdAt;
    }
}
