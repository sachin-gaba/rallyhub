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
public class Schedule {

    @DynamoDbPartitionKey
    private String id;

    private String clubId;
    private String name;
    private int dayOfWeek;   // 0=Sun … 6=Sat
    private String startTime; // HH:mm
    private String venue;
    private int capacityLimit;
    private Integer reducedCapacity;
    private int priceCredits;
    private boolean inductionSchedule;
    private Instant createdAt;
}
