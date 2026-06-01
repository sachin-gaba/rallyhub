package app.rallyhub.controller;

import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.TournamentService;
import app.rallyhub.util.JwtAuthUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

@Controller("/v1/clubs/{clubId}/tournaments")
public class TournamentController {

    @Inject TournamentService tournamentService;
    @Inject JwtAuthUtil jwtAuthUtil;

    @Post
    public HttpResponse<?> create(@Header("Authorization") String auth,
                                   @PathVariable String clubId,
                                   @Body Map<String, Object> body) {
        try {
            var ctx  = jwtAuthUtil.verify(auth);
            int gs   = body.containsKey("groupSize") ? ((Number) body.get("groupSize")).intValue() : 4;
            var t = tournamentService.createTournament(ctx.userId(), clubId,
                    (String) body.get("name"), (String) body.get("format"),
                    (String) body.get("drawType"), (String) body.get("sport"),
                    (List<String>) body.get("participantIds"), gs);
            return HttpResponse.created(t);
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Post("/{tournamentId}/matches/{matchId}/score")
    public HttpResponse<?> submitScore(@Header("Authorization") String auth,
                                        @PathVariable String clubId,
                                        @PathVariable String tournamentId,
                                        @PathVariable String matchId,
                                        @Body Map<String, Object> body) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            int s1  = ((Number) body.get("scorePlayer1")).intValue();
            int s2  = ((Number) body.get("scorePlayer2")).intValue();
            return HttpResponse.ok(tournamentService.submitScore(ctx.userId(), clubId, tournamentId, matchId, s1, s2));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Post("/{tournamentId}/matches/{matchId}/dispute")
    public HttpResponse<?> dispute(@Header("Authorization") String auth,
                                    @PathVariable String clubId,
                                    @PathVariable String tournamentId,
                                    @PathVariable String matchId,
                                    @Body Map<String, Object> body) {
        try {
            var ctx  = jwtAuthUtil.verify(auth);
            String note = (String) body.getOrDefault("note", "");
            return HttpResponse.ok(tournamentService.disputeScore(ctx.userId(), tournamentId, matchId, note));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    private HttpResponse<?> toResponse(RallyhubException e) {
        return HttpResponse.status(HttpStatus.valueOf(e.getStatus()))
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }
}
