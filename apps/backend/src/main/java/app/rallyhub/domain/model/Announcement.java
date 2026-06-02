package app.rallyhub.domain.model;
import io.micronaut.core.annotation.Introspected;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;

@Introspected
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Announcement {

    @Getter(onMethod_ = {@DynamoDbPartitionKey})
    private String id;

    private String clubId;
    private String authorId;
    private String body;
    private boolean pinned;
    private Instant createdAt;
    private Instant updatedAt;
}
