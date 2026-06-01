package app.rallyhub.controller;

import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.ICalService;
import app.rallyhub.util.JwtAuthUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;

import java.util.Map;

@Controller("/v1")
public class ICalController {

    @Inject ICalService icalService;
    @Inject JwtAuthUtil jwtAuthUtil;

    /** GET /v1/ical/{token} — public, authenticated by token */
    @Get(value = "/ical/{token}", produces = "text/calendar")
    public HttpResponse<?> getFeed(@PathVariable String token) {
        try {
            String ics = icalService.generateFeed(token);
            return HttpResponse.ok(ics)
                    .header("Content-Disposition", "attachment; filename=\"rallyhub.ics\"")
                    .header("Cache-Control", "no-cache, no-store");
        } catch (RallyhubException e) {
            return HttpResponse.notFound(Map.of("code", e.getCode(), "message", e.getMessage()));
        }
    }

    /** POST /v1/me/ical-token — rotate iCal token */
    @Post("/me/ical-token")
    public HttpResponse<?> rotateToken(@Header("Authorization") String auth) {
        try {
            var ctx      = jwtAuthUtil.verify(auth);
            String token = icalService.rotateIcalToken(ctx.userId());
            return HttpResponse.ok(Map.of("icalToken", token,
                    "feedUrl", "https://api.rallyhub.app/v1/ical/" + token));
        } catch (RallyhubException e) {
            return HttpResponse.status(io.micronaut.http.HttpStatus.valueOf(e.getStatus()))
                    .body(Map.of("code", e.getCode(), "message", e.getMessage()));
        }
    }
}
