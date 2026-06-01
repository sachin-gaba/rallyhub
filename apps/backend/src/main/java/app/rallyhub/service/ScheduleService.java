import jakarta.inject.Singleton;
package app.rallyhub.service;

import app.rallyhub.domain.model.Schedule;
import app.rallyhub.domain.model.Session;
import app.rallyhub.domain.repository.SessionRepository;
import app.rallyhub.exception.RallyhubException;
import lombok.RequiredArgsConstructor;

import software.amazon.awssdk.enhanced.dynamodb.*;

import java.time.*;
import java.util.*;

@Singleton
@RequiredArgsConstructor
public class ScheduleService {

    private final DynamoDbEnhancedClient ddb;
    private final SessionRepository sessionRepository;

    private DynamoDbTable<Schedule> scheduleTable(String tableName) {
        return ddb.table(tableName, TableSchema.fromBean(Schedule.class));
    }

    public Schedule createSchedule(String clubId, String name, int dayOfWeek,
                                    String startTime, String venue, int capacityLimit,
                                    int priceCredits, boolean isInduction) {
        Schedule schedule = Schedule.builder()
                .id(UUID.randomUUID().toString())
                .clubId(clubId)
                .name(name)
                .dayOfWeek(dayOfWeek)
                .startTime(startTime)
                .venue(venue)
                .capacityLimit(capacityLimit)
                .priceCredits(priceCredits)
                .inductionSchedule(isInduction)
                .createdAt(Instant.now())
                .build();
        scheduleTable("rallyhub-schedules").putItem(schedule);
        return schedule;
    }

    public Schedule getSchedule(String scheduleId) {
        Schedule s = scheduleTable("rallyhub-schedules")
                .getItem(Key.builder().partitionValue(scheduleId).build());
        if (s == null) throw RallyhubException.notFound("Schedule");
        return s;
    }

    /**
     * Generate the next N sessions for a schedule starting from today.
     * Called by a scheduled Lambda (e.g. weekly) to pre-create session records.
     */
    public List<Session> generateUpcomingSessions(Schedule schedule, int weeksAhead) {
        List<Session> created = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        for (int w = 0; w <= weeksAhead; w++) {
            LocalDate sessionDate = nextOccurrence(today.plusWeeks(w), schedule.getDayOfWeek());
            // Avoid duplicates
            String sessionId = schedule.getId() + "-" + sessionDate;
            Session session = Session.builder()
                    .id(sessionId)
                    .scheduleId(schedule.getId())
                    .clubId(schedule.getClubId())
                    .date(sessionDate)
                    .status("scheduled")
                    .attendees(new ArrayList<>())
                    .waitlist(new ArrayList<>())
                    .guestLedger(new ArrayList<>())
                    .createdAt(Instant.now())
                    .build();
            sessionRepository.save(session);
            created.add(session);
        }
        return created;
    }

    private LocalDate nextOccurrence(LocalDate from, int targetDow) {
        // DayOfWeek: 1=Mon … 7=Sun; spec uses 0=Sun … 6=Sat
        int fromDow  = from.getDayOfWeek().getValue() % 7; // convert to 0=Sun
        int diff     = (targetDow - fromDow + 7) % 7;
        return from.plusDays(diff == 0 ? 0 : diff);
    }
}
