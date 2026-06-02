package app.rallyhub.domain.repository;
import jakarta.inject.Singleton;

import app.rallyhub.config.DynamoTableConfig;
import app.rallyhub.domain.model.ClubMembership;
import lombok.RequiredArgsConstructor;

import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
public class MembershipRepository {

    private final DynamoDbEnhancedClient ddb;
    private final DynamoTableConfig tables;

    private DynamoDbTable<ClubMembership> table() {
        return ddb.table(tables.getMemberships(), TableSchema.fromBean(ClubMembership.class));
    }

    public ClubMembership save(ClubMembership membership) {
        table().putItem(membership);
        return membership;
    }

    public Optional<ClubMembership> findByUserAndClub(String userId, String clubId) {
        return Optional.ofNullable(table().getItem(
                Key.builder().partitionValue(userId).sortValue(clubId).build()));
    }

    public List<ClubMembership> findByClubId(String clubId) {
        // Requires GSI: clubId-index on memberships table
        DynamoDbIndex<ClubMembership> index = table().index("clubId-index");
        return index.query(QueryConditional.keyEqualTo(
                Key.builder().partitionValue(clubId).build()))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    public void update(ClubMembership membership) {
        table().updateItem(membership);
    }

    // All clubs a user belongs to (used by iCal feed generation)
    public java.util.List<ClubMembership> findByUserId(String userId) {
        return table().query(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo(
                Key.builder().partitionValue(userId).build()))
                .items().stream().collect(java.util.stream.Collectors.toList());
    }

}