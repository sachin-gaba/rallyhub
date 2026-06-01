package app.rallyhub.handler;

import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.TournamentService;
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
public class TournamentHandler {

    private final TournamentService tournamentService;
    private final JwtAuthUtil jwtAuthUtil;
    private final ApiResponse apiResponse;
    private final ObjectMapper mapper;

    /** POST /clubs/{clubId}/tournaments */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> createTournament() {
        return event -> {
            try {
                var auth   = jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                String clubId = event.getPathParameters().get("clubId");
                Map<?, ?> body = mapper.readValue(event.getBody(), Map.class);
                String name    = (String) body.get("name");
                String format  = (String) body.get("format");
                String draw    = (String) body.get("drawType");
                String sport   = (String) body.get("sport");
                List<String> pids = (List<String>) body.get("participantIds");
                int groupSize  = body.containsKey("groupSize") ? ((Number) body.get("groupSize")).intValue() : 4;
                var t = tournamentService.createTournament(auth.userId(), clubId, name, format, draw, sport, pids, groupSize);
                return apiResponse.created(t);
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }

    /** POST /clubs/{clubId}/tournaments/{tournamentId}/matches/{matchId}/score */
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> submitScore() {
        return event -> {
            try {
                var auth   = jwtAuthUtil.verify(event.getHeaders().get("Authorization"));
                var params = event.getPathParameters();
                Map<?, ?> body = mapper.readValue(event.getBody(), Map.class);
                int s1 = ((Number) body.get("scorePlayer1")).intValue();
                int s2 = ((Number) body.get("scorePlayer2")).intValue();
                var t = tournamentService.submitScore(auth.userId(), params.get("clubId"),
                        params.get("tournamentId"), params.get("matchId"), s1, s2);
                return apiResponse.ok(t);
            } catch (RallyhubException e) {
                return apiResponse.error(e.getCode(), e.getMessage(), e.getStatus());
            } catch (Exception e) {
                return apiResponse.error("INTERNAL", e.getMessage(), 500);
            }
        };
    }
}
