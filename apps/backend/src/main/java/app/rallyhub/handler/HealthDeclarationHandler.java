package app.rallyhub.handler;

import app.rallyhub.domain.model.HealthDeclaration;
import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.HealthDeclarationService;
import app.rallyhub.util.ApiResponse;
import app.rallyhub.util.JwtAuthUtil;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class HealthDeclarationHandler {

    private final HealthDeclarationService healthDeclarationService;
    private final JwtAuthUtil jwtAuthUtil;
    private final ApiResponse apiResponse;
    private final ObjectMapper mapper;

    /** POST /clubs/{clubId}/health-declaration */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> submitHealthDeclaration() {
        return event -> {
            try {
                var auth   = jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                String clubId = event.getPathParameters().get("clubId");
                Map<?, ?> body = mapper.readValue(event.getBody(), Map.class);

                Map<Integer, Boolean> parq = (Map<Integer, Boolean>) body.get("parqAnswers");
                List<String> conditions    = (List<String>) body.get("medicalConditions");
                String medications         = (String) body.getOrDefault("medications", "");
                Map<?, ?> ecMap            = (Map<?, ?>) body.get("emergencyContact");
                boolean liability          = Boolean.TRUE.equals(body.get("liabilityAccepted"));
                boolean dataConsent        = Boolean.TRUE.equals(body.get("dataProtectionConsented"));

                var ec = HealthDeclaration.EmergencyContact.builder()
                        .fullName((String) ecMap.get("fullName"))
                        .relationship((String) ecMap.get("relationship"))
                        .primaryPhone((String) ecMap.get("primaryPhone"))
                        .secondaryPhone((String) ecMap.get("secondaryPhone"))
                        .build();

                String deviceInfo  = event.getHeaders().getOrDefault("User-Agent", "");
                String appVersion  = event.getHeaders().getOrDefault("X-App-Version", "");

                var declaration = healthDeclarationService.submit(
                        auth.userId(), clubId, parq, conditions, medications,
                        ec, liability, dataConsent, deviceInfo, appVersion);

                return apiResponse.created(declaration);
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }

    /** PATCH /clubs/{clubId}/members/{userId}/promote */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> promoteToFullMember() {
        return event -> {
            try {
                var auth   = jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                var params = event.getPathParameters();
                var m = healthDeclarationService.promoteToFullMember(
                        auth.userId(), params.get("clubId"), params.get("userId"));
                return apiResponse.ok(Map.of("role", m.getRole()));
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }
}
