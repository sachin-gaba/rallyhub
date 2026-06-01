package app.rallyhub.util;

import app.rallyhub.exception.RallyhubException;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.JWSAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Map;

@Component
public class JwtAuthUtil {

    private final String userPoolId;
    private final String clientId;
    private final String region;

    public JwtAuthUtil(
            @Value("${rallyhub.cognito.user-pool-id}") String userPoolId,
            @Value("${rallyhub.cognito.client-id}")    String clientId,
            @Value("${rallyhub.aws.region}")           String region) {
        this.userPoolId = userPoolId;
        this.clientId   = clientId;
        this.region     = region;
    }

    public record AuthContext(String userId, String email) {}

    public AuthContext verify(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer "))
            throw RallyhubException.unauthorized();
        String token = bearerToken.substring(7);
        try {
            String jwksUrl = String.format(
                    "https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json", region, userPoolId);
            ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            processor.setJWSKeySelector(new JWSVerificationKeySelector<>(
                    JWSAlgorithm.RS256, new RemoteJWKSet<>(new URL(jwksUrl))));
            JWTClaimsSet claims = processor.process(token, null);
            String userId = claims.getSubject();
            String email  = (String) claims.getClaim("email");
            return new AuthContext(userId, email);
        } catch (Exception e) {
            throw RallyhubException.unauthorized();
        }
    }
}
