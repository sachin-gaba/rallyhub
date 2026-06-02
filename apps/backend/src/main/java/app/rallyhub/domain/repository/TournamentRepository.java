package app.rallyhub.domain.repository;
import jakarta.inject.Singleton;

import app.rallyhub.config.DynamoTableConfig;
import app.rallyhub.domain.model.Tournament;
import lombok.RequiredArgsConstructor;

import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
public class TournamentRepository {

    private final DynamoDbEnhancedClient ddb;
    private final DynamoTableConfig tables;

    private DynamoDbTable<Tournament> table() {
        return ddb.table(tables.getTournaments(), TableSchema.fromBean(Tournament.class));
    }

    public Tournament save(Tournament t) {
        table().putItem(t);
        return t;
    }

    public Optional<Tournament> findById(String id) {
        return Optional.ofNullable(table().getItem(Key.builder().partitionValue(id).build()));
    }

    public List<Tournament> findByClubId(String clubId) {
        DynamoDbIndex<Tournament> index = table().index("clubId-index");
        return index.query(QueryConditional.keyEqualTo(
                Key.builder().partitionValue(clubId).build()))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    public void update(Tournament t) {
        table().updateItem(t);
    }
}
