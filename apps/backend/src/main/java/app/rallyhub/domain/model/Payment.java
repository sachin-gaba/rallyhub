import io.micronaut.core.annotation.Introspected;
package app.rallyhub.domain.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;

@Introspected
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Payment {

    @DynamoDbPartitionKey
    private String id;

    private String userId;
    private String clubId;
    private double amount;          // GBP
    private String reference;       // member's unique payment reference
    private int    creditsAdded;    // set by organizer on verification
    private double creditPrice;     // GBP per credit at time of payment

    /** pending | verified | rejected */
    private String status;

    private String verifiedBy;      // organizer userId
    private Instant markedPaidAt;   // member tapped "Mark as Paid"
    private Instant verifiedAt;
    private String  note;
    private Instant createdAt;
}
