package app.rallyhub.domain.repository;
import jakarta.inject.Singleton;

import app.rallyhub.config.DynamoTableConfig;
import app.rallyhub.domain.model.Session;
import lombok.RequiredArgsConstructor;

import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
public class SessionRepository {

    private final DynamoDbEnhancedClient ddb;
    private final DynamoTableConfig tables;

    private DynamoDbTable<Session> table() {
        return ddb.table(tables.getSessions(), TableSchema.fromBean(Session.class));
    }

    public Session save(Session session) {
        table().putItem(session);
        return session;
    }

    public Optional<Session> findById(String id) {
        return Optional.ofNullable(table().getItem(Key.builder().partitionValue(id).build()));
    }

    public List<Session> findByClubId(String clubId) {
        DynamoDbIndex<Session> index = table().index("clubId-index");
        return index.query(QueryConditional.keyEqualTo(
                Key.builder().partitionValue(clubId).build()))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    public void update(Session session) {
        table().updateItem(session);
    }
}
