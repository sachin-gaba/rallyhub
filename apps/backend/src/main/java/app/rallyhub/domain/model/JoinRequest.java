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
public class JoinRequest {

    @DynamoDbPartitionKey
    private String id;

    private String userId;
    private String clubId;

    /** pending | accepted | declined */
    private String status;

    private String reviewedBy;
    private String reviewNote;
    private Instant requestedAt;
    private Instant reviewedAt;
}
