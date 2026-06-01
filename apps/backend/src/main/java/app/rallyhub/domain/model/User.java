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
public class User {

    @DynamoDbPartitionKey
    private String id;          // Cognito sub

    private String email;
    private String displayName;
    private String phone;       // optional
    private Instant createdAt;
    private Instant termsAcceptedAt;
    private String platformRole; // null for regular users, "platform_admin" for staff
    private String pushToken;    // SNS endpoint ARN for this device
    private String icalToken;    // per-user secret for iCal feed
    private NotificationPreferences notificationPreferences;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor @DynamoDbBean
    public static class NotificationPreferences {
        private boolean sessionReminders;
        private int[]   sessionReminderLeadHours;  // e.g. [24, 2]
        private boolean waitlistUpdates;
        private boolean creditAlerts;
        private boolean clubAnnouncements;
        private boolean tournamentUpdates;
        private boolean emailFallbackOnly;          // for time-critical events
    }
}
