package app.rallyhub.controller;

import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.ClubService;
import app.rallyhub.util.JwtAuthUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

@Controller("/v1/clubs")
public class ClubController {

    @Inject ClubService clubService;
    @Inject JwtAuthUtil jwtAuthUtil;

    @Post
    public HttpResponse<?> createClub(@Header("Authorization") String auth,
                                       @Body Map<String, Object> body) {
        try {
            var ctx     = jwtAuthUtil.verify(auth);
            String name = (String) body.get("name");
            List<String> sports = (List<String>) body.get("sports");
            String venue = (String) body.get("primaryVenue");
            if (name == null || sports == null || venue == null)
                return HttpResponse.badRequest(Map.of("code", "VALIDATION", "message", "name, sports, primaryVenue required"));
            return HttpResponse.created(clubService.createClub(ctx.userId(), name, sports, venue));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Get("/{clubId}")
    public HttpResponse<?> getClub(@Header("Authorization") String auth,
                                    @PathVariable String clubId) {
        try {
            jwtAuthUtil.verify(auth);
            return HttpResponse.ok(clubService.getClub(clubId));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Get("/invite/{inviteCode}")
    public HttpResponse<?> getByInviteCode(@PathVariable String inviteCode) {
        try {
            return HttpResponse.ok(clubService.getClubByInviteCode(inviteCode));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    private HttpResponse<?> toResponse(RallyhubException e) {
        return HttpResponse.status(io.micronaut.http.HttpStatus.valueOf(e.getStatus()))
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }
}
