package app.rallyhub.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApiResponse {

    private final ObjectMapper mapper;

    private static final Map<String, String> CORS = Map.of(
            "Access-Control-Allow-Origin",  "*",
            "Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS",
            "Access-Control-Allow-Headers", "Content-Type,Authorization"
    );

    public APIGatewayProxyResponseEvent ok(Object data) {
        return ok(data, 200);
    }

    public APIGatewayProxyResponseEvent ok(Object data, int status) {
        return build(status, Map.of("data", data));
    }

    public APIGatewayProxyResponseEvent created(Object data) {
        return ok(data, 201);
    }

    public APIGatewayProxyResponseEvent error(String code, String message, int status) {
        return build(status, Map.of("error", Map.of("code", code, "message", message)));
    }

    private APIGatewayProxyResponseEvent build(int status, Object body) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(status)
                    .withHeaders(CORS)
                    .withBody(mapper.writeValueAsString(body));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(CORS)
                    .withBody("{\"error\":{\"code\":\"SERIALIZATION\",\"message\":\"Response serialization failed\"}}");
        }
    }
}
