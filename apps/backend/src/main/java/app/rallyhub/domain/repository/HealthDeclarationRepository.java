package app.rallyhub.domain.repository;

import app.rallyhub.config.DynamoTableConfig;
import app.rallyhub.domain.model.HealthDeclaration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HealthDeclarationRepository {

    private final DynamoDbEnhancedClient ddb;
    private final DynamoTableConfig tables;

    private DynamoDbTable<HealthDeclaration> table() {
        return ddb.table(tables.getDeclarations(), TableSchema.fromBean(HealthDeclaration.class));
    }

    public HealthDeclaration save(HealthDeclaration declaration) {
        table().putItem(declaration);
        return declaration;
    }

    public Optional<HealthDeclaration> findById(String id) {
        return Optional.ofNullable(table().getItem(Key.builder().partitionValue(id).build()));
    }
}
