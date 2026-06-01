package app.rallyhub.controller;

import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.SessionService;
import app.rallyhub.util.JwtAuthUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import java.util.Map;

@Controller("/v1/clubs/{clubId}/sessions")
public class SessionController {

    @Inject SessionService sessionService;
    @Inject JwtAuthUtil jwtAuthUtil;

    @Post("/{sessionId}/book")
    public HttpResponse<?> book(@Header("Authorization") String auth,
                                 @PathVariable String clubId,
                                 @PathVariable String sessionId) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            sessionService.bookSession(ctx.userId(), clubId, sessionId);
            return HttpResponse.ok(Map.of("booked", true));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Delete("/{sessionId}/book")
    public HttpResponse<?> cancelBooking(@Header("Authorization") String auth,
                                          @PathVariable String clubId,
                                          @PathVariable String sessionId) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            sessionService.cancelBooking(ctx.userId(), clubId, sessionId);
            return HttpResponse.ok(Map.of("cancelled", true));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Post("/{sessionId}/waitlist")
    public HttpResponse<?> joinWaitlist(@Header("Authorization") String auth,
                                         @PathVariable String clubId,
                                         @PathVariable String sessionId) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            var s   = sessionService.joinWaitlist(ctx.userId(), clubId, sessionId);
            int pos = s.getWaitlist().stream()
                    .filter(w -> w.getUserId().equals(ctx.userId()))
                    .findFirst().map(w -> w.getPosition()).orElse(-1);
            return HttpResponse.ok(Map.of("position", pos));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Patch("/{sessionId}/complete")
    public HttpResponse<?> complete(@Header("Authorization") String auth,
                                     @PathVariable String clubId,
                                     @PathVariable String sessionId) {
        try {
            jwtAuthUtil.verify(auth);
            sessionService.markComplete(sessionId);
            return HttpResponse.ok(Map.of("status", "completed"));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Patch("/{sessionId}/cancel")
    public HttpResponse<?> cancel(@Header("Authorization") String auth,
                                   @PathVariable String clubId,
                                   @PathVariable String sessionId,
                                   @Body Map<String, Object> body) {
        try {
            jwtAuthUtil.verify(auth);
            String reason = (String) body.getOrDefault("reason", "");
            sessionService.cancelSession(sessionId, reason);
            return HttpResponse.ok(Map.of("status", "cancelled"));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    private HttpResponse<?> toResponse(RallyhubException e) {
        return HttpResponse.status(HttpStatus.valueOf(e.getStatus()))
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }
}
