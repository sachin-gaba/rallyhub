package app.rallyhub.handler;

import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.SessionService;
import app.rallyhub.util.ApiResponse;
import app.rallyhub.util.JwtAuthUtil;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class SessionHandler {

    private final SessionService sessionService;
    private final JwtAuthUtil jwtAuthUtil;
    private final ApiResponse apiResponse;
    private final ObjectMapper mapper;

    /** POST /clubs/{clubId}/sessions/{sessionId}/book */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> bookSession() {
        return event -> {
            try {
                var auth      = jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                var params    = event.getPathParameters();
                var session   = sessionService.bookSession(auth.userId(), params.get("clubId"), params.get("sessionId"));
                return apiResponse.ok(Map.of("booked", true, "sessionId", session.getId()));
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }

    /** DELETE /clubs/{clubId}/sessions/{sessionId}/book */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> cancelBooking() {
        return event -> {
            try {
                var auth   = jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                var params = event.getPathParameters();
                sessionService.cancelBooking(auth.userId(), params.get("clubId"), params.get("sessionId"));
                return apiResponse.ok(Map.of("cancelled", true));
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }

    /** POST /clubs/{clubId}/sessions/{sessionId}/waitlist */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> joinWaitlist() {
        return event -> {
            try {
                var auth    = jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                var params  = event.getPathParameters();
                var session = sessionService.joinWaitlist(auth.userId(), params.get("clubId"), params.get("sessionId"));
                int pos     = session.getWaitlist().stream()
                        .filter(w -> w.getUserId().equals(auth.userId()))
                        .findFirst().map(w -> w.getPosition()).orElse(-1);
                return apiResponse.ok(Map.of("position", pos));
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }

    /** PATCH /clubs/{clubId}/sessions/{sessionId}/complete */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> completeSession() {
        return event -> {
            try {
                jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                String sessionId = event.getPathParameters().get("sessionId");
                sessionService.markComplete(sessionId);
                return apiResponse.ok(Map.of("status", "completed"));
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }

    /** PATCH /clubs/{clubId}/sessions/{sessionId}/cancel */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> cancelSession() {
        return event -> {
            try {
                jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                var params = event.getPathParameters();
                Map<?, ?> body = mapper.readValue(event.getBody() != null ? event.getBody() : "{}", Map.class);
                String reason  = (String) body.getOrDefault("reason", "");
                sessionService.cancelSession(params.get("sessionId"), reason);
                return apiResponse.ok(Map.of("status", "cancelled"));
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }
}
