package app.rallyhub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rallyhub.dynamodb.table")
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
}
