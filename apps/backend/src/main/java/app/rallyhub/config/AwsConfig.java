package app.rallyhub.config;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;

@Factory
public class AwsConfig {

    @Value("${rallyhub.aws.region}")
    private String region;

    @Singleton
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                // UrlConnectionHttpClient is GraalVM-native compatible (no Netty)
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    @Singleton
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Singleton
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    @Singleton
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }
}
