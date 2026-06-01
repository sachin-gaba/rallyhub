package app.rallyhub.service;

import app.rallyhub.domain.model.Schedule;
import app.rallyhub.domain.model.Session;
import app.rallyhub.domain.repository.SessionRepository;
import app.rallyhub.exception.RallyhubException;
import io.micronaut.core.annotation.Introspected;
import jakarta.inject.Singleton;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ScheduleService {

    private final DynamoDbEnhancedClient ddb;
    private final SessionRepository sessionRepository;

    private static final String TABLE = "rallyhub-schedules";

    public ScheduleService(DynamoDbEnhancedClient ddb, SessionRepository sessionRepository) {
        this.ddb = ddb;
        this.sessionRepository = sessionRepository;
    }

    private DynamoDbTable<Schedule> table() {
        return ddb.table(TABLE, TableSchema.fromBean(Schedule.class));
    }

    public Schedule createSchedule(String clubId, String name, int dayOfWeek,
                                    String startTime, String venue, int capacityLimit,
                                    int priceCredits, boolean isInduction) {
        Schedule s = Schedule.builder()
                .id(UUID.randomUUID().toString())
                .clubId(clubId).name(name).dayOfWeek(dayOfWeek)
                .startTime(startTime).venue(venue).capacityLimit(capacityLimit)
                .priceCredits(priceCredits).inductionSchedule(isInduction)
                .createdAt(Instant.now())
                .build();
        table().putItem(s);
        return s;
    }

    public Schedule getSchedule(String scheduleId) {
        Schedule s = table().getItem(Key.builder().partitionValue(scheduleId).build());
        if (s == null) throw RallyhubException.notFound("Schedule");
        return s;
    }

    public List<Schedule> listForClub(String clubId) {
        DynamoDbIndex<Schedule> index = table().index("clubId-index");
        return index.query(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(clubId).build()))
                .stream().flatMap(p -> p.items().stream())
                .sorted(Comparator.comparing(Schedule::getDayOfWeek))
                .collect(Collectors.toList());
    }

    public Schedule updateSchedule(String scheduleId, Map<String, Object> updates) {
        Schedule s = getSchedule(scheduleId);
        if (updates.containsKey("name"))          s.setName((String) updates.get("name"));
        if (updates.containsKey("venue"))         s.setVenue((String) updates.get("venue"));
        if (updates.containsKey("startTime"))     s.setStartTime((String) updates.get("startTime"));
        if (updates.containsKey("capacityLimit")) s.setCapacityLimit(((Number) updates.get("capacityLimit")).intValue());
        if (updates.containsKey("priceCredits"))  s.setPriceCredits(((Number) updates.get("priceCredits")).intValue());
        if (updates.containsKey("reducedCapacity")) {
            Object rc = updates.get("reducedCapacity");
            s.setReducedCapacity(rc == null ? null : ((Number) rc).intValue());
        }
        table().updateItem(s);
        return s;
    }

    /**
     * Generate upcoming session records for a schedule.
     * Called by EventBridge weekly rule or organizer manually.
     */
    public List<Session> generateUpcomingSessions(Schedule schedule, int weeksAhead) {
        List<Session> created = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        for (int w = 0; w <= weeksAhead; w++) {
            LocalDate sessionDate = nextOccurrence(today.plusWeeks(w), schedule.getDayOfWeek());
            String sessionId = schedule.getId() + "-" + sessionDate;

            // Idempotent: skip if already exists
            if (sessionRepository.findById(sessionId).isPresent()) continue;

            Session session = Session.builder()
                    .id(sessionId).scheduleId(schedule.getId()).clubId(schedule.getClubId())
                    .date(sessionDate).status("scheduled")
                    .attendees(new ArrayList<>()).waitlist(new ArrayList<>())
                    .guestLedger(new ArrayList<>()).createdAt(Instant.now())
                    .build();
            sessionRepository.save(session);
            created.add(session);
        }
        return created;
    }

    /**
     * Resolve effective capacity for a session — reduced capacity overrides schedule limit.
     */
    public int resolveCapacity(Session session) {
        if (session.getScheduleId() == null) return 20;
        try {
            Schedule s = getSchedule(session.getScheduleId());
            return s.getReducedCapacity() != null ? s.getReducedCapacity() : s.getCapacityLimit();
        } catch (RallyhubException e) {
            return 20; // fallback
        }
    }

    private LocalDate nextOccurrence(LocalDate from, int targetDow) {
        // spec: 0=Sun … 6=Sat; Java DayOfWeek: Mon=1 … Sun=7
        int fromDow = from.getDayOfWeek().getValue() % 7;
        int diff    = (targetDow - fromDow + 7) % 7;
        return from.plusDays(diff);
    }
}
