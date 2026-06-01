package app.rallyhub.handler;

import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.ClubService;
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
public class ClubHandler {

    private final ClubService clubService;
    private final JwtAuthUtil jwtAuthUtil;
    private final ApiResponse apiResponse;
    private final ObjectMapper mapper;

    /** POST /clubs */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> createClub() {
        return event -> {
            try {
                var auth = jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                Map<?, ?> body = mapper.readValue(event.getBody(), Map.class);
                String name         = (String) body.get("name");
                List<String> sports = (List<String>) body.get("sports");
                String venue        = (String) body.get("primaryVenue");
                if (name == null || sports == null || venue == null)
                    return apiResponse.error("VALIDATION", "name, sports, primaryVenue required", 400);
                var club = clubService.createClub(auth.userId(), name, sports, venue);
                return apiResponse.created(club);
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }

    /** GET /clubs/{clubId} */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getClub() {
        return event -> {
            try {
                jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                String clubId = event.getPathParameters().get("clubId");
                return apiResponse.ok(clubService.getClub(clubId));
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }

    /** GET /clubs/invite/{inviteCode} */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getClubByInviteCode() {
        return event -> {
            try {
                String code = event.getPathParameters().get("inviteCode");
                return apiResponse.ok(clubService.getClubByInviteCode(code));
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }
}
