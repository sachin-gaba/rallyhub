package app.rallyhub.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;

import java.util.Map;

@Singleton
public class ApiResponse {

    private static final Map<String, String> CORS = Map.of(
            "Access-Control-Allow-Origin",  "*",
            "Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS",
            "Access-Control-Allow-Headers", "Content-Type,Authorization"
    );

    private final ObjectMapper mapper;

    public ApiResponse(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public APIGatewayProxyResponseEvent ok(Object data) {
        return build(200, Map.of("data", data));
    }

    public APIGatewayProxyResponseEvent created(Object data) {
        return build(201, Map.of("data", data));
    }

    public APIGatewayProxyResponseEvent noContent() {
        return new APIGatewayProxyResponseEvent().withStatusCode(204).withHeaders(CORS).withBody("");
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
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withHeaders(CORS)
                    .withBody("{\"error\":{\"code\":\"SERIALIZATION\",\"message\":\"Serialization failed\"}}");
        }
    }
}
