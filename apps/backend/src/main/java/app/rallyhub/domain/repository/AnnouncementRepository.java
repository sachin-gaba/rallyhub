import jakarta.inject.Singleton;
package app.rallyhub.domain.repository;

import app.rallyhub.config.DynamoTableConfig;
import app.rallyhub.domain.model.Announcement;
import lombok.RequiredArgsConstructor;

import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
public class AnnouncementRepository {

    private final DynamoDbEnhancedClient ddb;
    private final DynamoTableConfig tables;

    private DynamoDbTable<Announcement> table() {
        return ddb.table("rallyhub-announcements", TableSchema.fromBean(Announcement.class));
    }

    public Announcement save(Announcement a) { table().putItem(a); return a; }

    public List<Announcement> findByClubId(String clubId) {
        DynamoDbIndex<Announcement> index = table().index("clubId-index");
        return index.query(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(clubId).build()))
                .stream()
                .flatMap(p -> p.items().stream())
                .sorted((a, b) -> {
                    if (a.isPinned() && !b.isPinned()) return -1;
                    if (!a.isPinned() && b.isPinned()) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    public void update(Announcement a) { table().updateItem(a); }
}
