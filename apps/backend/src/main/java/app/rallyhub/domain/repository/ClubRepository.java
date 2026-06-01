package app.rallyhub.domain.repository;

import app.rallyhub.config.DynamoTableConfig;
import app.rallyhub.domain.model.Club;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class ClubRepository {

    private final DynamoDbEnhancedClient ddb;
    private final DynamoTableConfig tables;

    private DynamoDbTable<Club> table() {
        return ddb.table(tables.getClubs(), TableSchema.fromBean(Club.class));
    }

    public Club save(Club club) {
        table().putItem(club);
        return club;
    }

    public Optional<Club> findById(String id) {
        return Optional.ofNullable(table().getItem(Key.builder().partitionValue(id).build()));
    }

    public Optional<Club> findByInviteCode(String inviteCode) {
        // Uses GSI inviteCode-index
        DynamoDbIndex<Club> index = table().index("inviteCode-index");
        var results = index.query(QueryConditional.keyEqualTo(
                Key.builder().partitionValue(inviteCode).build())).iterator();
        if (!results.hasNext()) return Optional.empty();
        return results.next().items().stream().findFirst();
    }

    public void update(Club club) {
        table().updateItem(club);
    }
}
