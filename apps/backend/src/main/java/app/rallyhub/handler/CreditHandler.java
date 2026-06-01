package app.rallyhub.handler;

import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.CreditService;
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
public class CreditHandler {

    private final CreditService creditService;
    private final JwtAuthUtil jwtAuthUtil;
    private final ApiResponse apiResponse;
    private final ObjectMapper mapper;

    /** POST /clubs/{clubId}/credits/{userId}/adjust */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> adjustCredit() {
        return event -> {
            try {
                var auth   = jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                var params = event.getPathParameters();
                Map<?, ?> body  = mapper.readValue(event.getBody(), Map.class);
                int amount      = ((Number) body.get("amount")).intValue();
                String note     = (String) body.getOrDefault("note", "");
                var membership  = creditService.adjustCredit(auth.userId(), params.get("clubId"), params.get("userId"), amount, note);
                return apiResponse.ok(Map.of("newBalance", membership.getCreditBalance()));
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }

    /** GET /clubs/{clubId}/ledger/me */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getMyLedger() {
        return event -> {
            try {
                var auth   = jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                String clubId = event.getPathParameters().get("clubId");
                var entries = creditService.getLedger(auth.userId(), clubId);
                return apiResponse.ok(Map.of("items", entries));
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }
}
