package app.rallyhub.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("rallyhub.dynamodb.table")
public class DynamoTableConfig {
    private String users;
    private String clubs;
    private String memberships;
    private String sessions;
    private String schedules;
    private String ledger;
    private String tournaments;
    private String declarations;
    private String payments;
    private String announcements;
    private String joinRequests;
}
