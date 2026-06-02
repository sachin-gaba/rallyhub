package app.rallyhub.domain.model;
import io.micronaut.core.annotation.Introspected;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Introspected
@DynamoDbBean
public class HealthDeclaration {

    @Getter(onMethod_ = {@DynamoDbPartitionKey})
    private String id;

    private String userId;
    private String clubId;

    /** PAR-Q answers: key = question index (0-6), value = true=Yes, false=No */
    private Map<Integer, Boolean> parqAnswers;

    private List<String> medicalConditions;
    private String medications;
    private EmergencyContact emergencyContact;
    private boolean liabilityAccepted;
    private boolean dataProtectionConsented;
    private Instant submittedAt;
    private String deviceInfo;
    private String appVersion;
    private String pdfUrl;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor @Introspected
@DynamoDbBean
    public static class EmergencyContact {
        private String fullName;
        private String relationship;
        private String primaryPhone;
        private String secondaryPhone;
    }
}
