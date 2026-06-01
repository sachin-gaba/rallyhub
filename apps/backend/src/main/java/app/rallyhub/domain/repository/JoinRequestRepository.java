import jakarta.inject.Singleton;
package app.rallyhub.domain.repository;

import app.rallyhub.config.DynamoTableConfig;
import app.rallyhub.domain.model.JoinRequest;
import lombok.RequiredArgsConstructor;

import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
public class JoinRequestRepository {

    private final DynamoDbEnhancedClient ddb;
    private final DynamoTableConfig tables;

    private DynamoDbTable<JoinRequest> table() {
        return ddb.table("rallyhub-join-requests", TableSchema.fromBean(JoinRequest.class));
    }

    public JoinRequest save(JoinRequest r) { table().putItem(r); return r; }

    public Optional<JoinRequest> findById(String id) {
        return Optional.ofNullable(table().getItem(Key.builder().partitionValue(id).build()));
    }

    public List<JoinRequest> findPendingByClub(String clubId) {
        DynamoDbIndex<JoinRequest> index = table().index("clubId-index");
        return index.query(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(clubId).build()))
                .stream()
                .flatMap(p -> p.items().stream())
                .filter(r -> "pending".equals(r.getStatus()))
                .collect(Collectors.toList());
    }

    public void update(JoinRequest r) { table().updateItem(r); }
}
