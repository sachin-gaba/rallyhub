import jakarta.inject.Singleton;
package app.rallyhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Set;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class NotificationService {

    private final SnsClient sns;
    private final SesClient ses;

    @Value("${rallyhub.sns.topic-arn:}")
    private String snsTopicArn;

    private static final String FROM_EMAIL = "noreply@rallyhub.app";

    /** Events that fall back to email if push fails */
    private static final Set<String> TIME_CRITICAL = Set.of(
            "waitlist_slot_sequential",
            "waitlist_panic_window",
            "session_cancelled"
    );

    public void send(String pushEndpointArn, String email, String event,
                     String title, String body) {
        boolean pushSent = trySendPush(pushEndpointArn, title, body);

        if (!pushSent && email != null && TIME_CRITICAL.contains(event)) {
            trySendEmail(email, title, body);
        }
    }

    private boolean trySendPush(String endpointArn, String title, String body) {
        if (endpointArn == null || endpointArn.isBlank()) return false;
        try {
            String apnsPayload  = String.format(
                    "{\"aps\":{\"alert\":{\"title\":\"%s\",\"body\":\"%s\"},\"sound\":\"default\"}}",
                    escape(title), escape(body));
            String gcmPayload   = String.format(
                    "{\"notification\":{\"title\":\"%s\",\"body\":\"%s\"}}",
                    escape(title), escape(body));
            String message = String.format(
                    "{\"default\":\"%s\",\"APNS\":%s,\"GCM\":%s}",
                    escape(body), apnsPayload, gcmPayload);

            sns.publish(PublishRequest.builder()
                    .targetArn(endpointArn)
                    .message(message)
                    .messageStructure("json")
                    .build());
            return true;
        } catch (Exception e) {
            log.warn("Push failed for endpoint {}: {}", endpointArn, e.getMessage());
            return false;
        }
    }

    private void trySendEmail(String toAddress, String subject, String bodyText) {
        try {
            ses.sendEmail(SendEmailRequest.builder()
                    .source(FROM_EMAIL)
                    .destination(Destination.builder().toAddresses(toAddress).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder()
                                    .text(Content.builder().data(bodyText).build())
                                    .build())
                            .build())
                    .build());
        } catch (Exception e) {
            log.warn("Email fallback failed for {}: {}", toAddress, e.getMessage());
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"").replace("\n", "\\n");
    }
}
