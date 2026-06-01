package app.rallyhub.domain.repository;

import app.rallyhub.config.DynamoTableConfig;
import app.rallyhub.domain.model.CreditLedgerEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CreditLedgerRepository {

    private final DynamoDbEnhancedClient ddb;
    private final DynamoTableConfig tables;

    private DynamoDbTable<CreditLedgerEntry> table() {
        return ddb.table(tables.getLedger(), TableSchema.fromBean(CreditLedgerEntry.class));
    }

    public CreditLedgerEntry save(CreditLedgerEntry entry) {
        table().putItem(entry);
        return entry;
    }

    public List<CreditLedgerEntry> findByUserAndClub(String userId, String clubId) {
        // GSI: userId-clubId-index
        DynamoDbIndex<CreditLedgerEntry> index = table().index("userId-clubId-index");
        return index.query(QueryConditional.keyEqualTo(
                Key.builder().partitionValue(userId).sortValue(clubId).build()))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }
}
