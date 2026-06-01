import io.micronaut.core.annotation.Introspected;
package app.rallyhub.domain.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Introspected
@DynamoDbBean
public class CreditLedgerEntry {

    @DynamoDbPartitionKey
    private String id;

    private String userId;
    private String clubId;

    /**
     * top_up | session_deduction | cancellation_refund |
     * organizer_cancellation_refund | correction | negative_limit_release
     */
    private String type;

    private int amount;        // positive = credit, negative = debit
    private int balanceAfter;
    private String note;
    private String createdBy;  // userId of actor
    private String sessionId;
    private Instant createdAt;
}
