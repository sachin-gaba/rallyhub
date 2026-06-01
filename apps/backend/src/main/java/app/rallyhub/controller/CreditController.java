package app.rallyhub.controller;

import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.CreditService;
import app.rallyhub.util.JwtAuthUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import java.util.Map;

@Controller("/v1/clubs/{clubId}")
public class CreditController {

    @Inject CreditService creditService;
    @Inject JwtAuthUtil jwtAuthUtil;

    @Post("/credits/{userId}/adjust")
    public HttpResponse<?> adjust(@Header("Authorization") String auth,
                                   @PathVariable String clubId,
                                   @PathVariable String userId,
                                   @Body Map<String, Object> body) {
        try {
            var ctx    = jwtAuthUtil.verify(auth);
            int amount = ((Number) body.get("amount")).intValue();
            String note = (String) body.getOrDefault("note", "");
            var m = creditService.adjustCredit(ctx.userId(), clubId, userId, amount, note);
            return HttpResponse.ok(Map.of("newBalance", m.getCreditBalance()));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Get("/ledger/me")
    public HttpResponse<?> myLedger(@Header("Authorization") String auth,
                                     @PathVariable String clubId) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            var entries = creditService.getLedger(ctx.userId(), clubId);
            return HttpResponse.ok(Map.of("items", entries, "count", entries.size()));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    private HttpResponse<?> toResponse(RallyhubException e) {
        return HttpResponse.status(HttpStatus.valueOf(e.getStatus()))
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }
}
