package app.rallyhub;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.micronaut.function.aws.proxy.MockLambdaContext;
import io.micronaut.function.aws.proxy.MicronautLambdaHandler;

/**
 * Lambda entry point.
 * CDK handler string: app.rallyhub.Handler
 * Micronaut routes all API Gateway requests to the appropriate @Controller method.
 */
public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final MicronautLambdaHandler HANDLER;

    static {
        try {
            HANDLER = new MicronautLambdaHandler();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise Micronaut Lambda handler", e);
        }
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        return HANDLER.handleRequest(request, context);
    }
}
