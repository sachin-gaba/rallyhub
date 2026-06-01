import jakarta.inject.Singleton;
package app.rallyhub.domain.repository;

import app.rallyhub.config.DynamoTableConfig;
import app.rallyhub.domain.model.User;
import lombok.RequiredArgsConstructor;

import software.amazon.awssdk.enhanced.dynamodb.*;

import java.util.Optional;

@Singleton
@RequiredArgsConstructor
public class UserRepository {

    private final DynamoDbEnhancedClient ddb;
    private final DynamoTableConfig tables;

    private DynamoDbTable<User> table() {
        return ddb.table(tables.getUsers(), TableSchema.fromBean(User.class));
    }

    public User save(User user) { table().putItem(user); return user; }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(table().getItem(Key.builder().partitionValue(id).build()));
    }

    public void update(User user) { table().updateItem(user); }
}

    // Used by iCal feed lookup
    public java.util.Optional<User> findByIcalToken(String icalToken) {
        // Requires GSI: icalToken-index on users table
        DynamoDbIndex<User> index = table().index("icalToken-index");
        return index.query(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo(
                Key.builder().partitionValue(icalToken).build()))
                .stream().flatMap(p -> p.items().stream()).findFirst();
    }
