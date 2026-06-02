package app.rallyhub.domain.repository;
import jakarta.inject.Singleton;

import app.rallyhub.config.DynamoTableConfig;
import app.rallyhub.domain.model.Payment;
import lombok.RequiredArgsConstructor;

import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
public class PaymentRepository {

    private final DynamoDbEnhancedClient ddb;
    private final DynamoTableConfig tables;

    private DynamoDbTable<Payment> table() {
        return ddb.table(tables.getPayments(), TableSchema.fromBean(Payment.class));
    }

    public Payment save(Payment p) { table().putItem(p); return p; }

    public Optional<Payment> findById(String id) {
        return Optional.ofNullable(table().getItem(Key.builder().partitionValue(id).build()));
    }

    public List<Payment> findPendingByClub(String clubId) {
        // GSI: clubId-status-index
        DynamoDbIndex<Payment> index = table().index("clubId-status-index");
        return index.query(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(clubId).build()))
                .stream()
                .flatMap(p -> p.items().stream())
                .filter(p -> "pending".equals(p.getStatus()))
                .collect(Collectors.toList());
    }

    public void update(Payment p) { table().updateItem(p); }
}
