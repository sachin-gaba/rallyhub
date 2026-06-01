package app.rallyhub.controller;

import app.rallyhub.domain.model.Schedule;
import app.rallyhub.domain.repository.MembershipRepository;
import app.rallyhub.domain.repository.SessionRepository;
import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.ScheduleService;
import app.rallyhub.util.JwtAuthUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@Controller("/v1/clubs/{clubId}/schedules")
public class ScheduleController {

    @Inject ScheduleService scheduleService;
    @Inject MembershipRepository membershipRepository;
    @Inject JwtAuthUtil jwtAuthUtil;

    private static final List<String> ORG_ROLES =
            List.of("organizer_primary", "organizer_additional", "co_organizer");

    /** GET /clubs/{clubId}/schedules */
    @Get
    public HttpResponse<?> list(@Header("Authorization") String auth,
                                 @PathVariable String clubId) {
        try {
            jwtAuthUtil.verify(auth);
            return HttpResponse.ok(Map.of("items", scheduleService.listForClub(clubId)));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    /** GET /clubs/{clubId}/schedules/{scheduleId} */
    @Get("/{scheduleId}")
    public HttpResponse<?> get(@Header("Authorization") String auth,
                                @PathVariable String clubId,
                                @PathVariable String scheduleId) {
        try {
            jwtAuthUtil.verify(auth);
            return HttpResponse.ok(scheduleService.getSchedule(scheduleId));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    /** POST /clubs/{clubId}/schedules */
    @Post
    public HttpResponse<?> create(@Header("Authorization") String auth,
                                   @PathVariable String clubId,
                                   @Body Map<String, Object> body) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            membershipRepository.findByUserAndClub(ctx.userId(), clubId)
                    .filter(m -> ORG_ROLES.contains(m.getRole()))
                    .orElseThrow(() -> RallyhubException.forbidden("Only organisers can create schedules"));

            String name   = (String) body.get("name");
            int    dow    = ((Number) body.get("dayOfWeek")).intValue();
            String time   = (String) body.get("startTime");
            String venue  = (String) body.get("venue");
            int    cap    = ((Number) body.get("capacityLimit")).intValue();
            int    price  = ((Number) body.get("priceCredits")).intValue();
            boolean induc = Boolean.TRUE.equals(body.get("isInduction"));

            var s = scheduleService.createSchedule(clubId, name, dow, time, venue, cap, price, induc);
            return HttpResponse.created(s);
        } catch (RallyhubException e) { return toResponse(e); }
    }

    /** PATCH /clubs/{clubId}/schedules/{scheduleId} */
    @Patch("/{scheduleId}")
    public HttpResponse<?> update(@Header("Authorization") String auth,
                                   @PathVariable String clubId,
                                   @PathVariable String scheduleId,
                                   @Body Map<String, Object> body) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            membershipRepository.findByUserAndClub(ctx.userId(), clubId)
                    .filter(m -> ORG_ROLES.contains(m.getRole()))
                    .orElseThrow(() -> RallyhubException.forbidden("Only organisers can update schedules"));
            var s = scheduleService.updateSchedule(scheduleId, body);
            return HttpResponse.ok(s);
        } catch (RallyhubException e) { return toResponse(e); }
    }

    /** POST /clubs/{clubId}/schedules/{scheduleId}/generate-sessions */
    @Post("/{scheduleId}/generate-sessions")
    public HttpResponse<?> generateSessions(@Header("Authorization") String auth,
                                             @PathVariable String clubId,
                                             @PathVariable String scheduleId,
                                             @Body Map<String, Object> body) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            membershipRepository.findByUserAndClub(ctx.userId(), clubId)
                    .filter(m -> ORG_ROLES.contains(m.getRole()))
                    .orElseThrow(() -> RallyhubException.forbidden("Only organisers can generate sessions"));
            int weeks = body.containsKey("weeksAhead") ? ((Number) body.get("weeksAhead")).intValue() : 4;
            var schedule = scheduleService.getSchedule(scheduleId);
            var sessions = scheduleService.generateUpcomingSessions(schedule, weeks);
            return HttpResponse.created(Map.of("created", sessions.size(), "sessions", sessions));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    private HttpResponse<?> toResponse(RallyhubException e) {
        return HttpResponse.status(HttpStatus.valueOf(e.getStatus()))
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }
}
