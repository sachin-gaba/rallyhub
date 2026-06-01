package app.rallyhub.controller;

import app.rallyhub.domain.model.Payment;
import app.rallyhub.domain.repository.*;
import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.*;
import app.rallyhub.util.JwtAuthUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.*;

@Controller("/v1/clubs/{clubId}/payments")
public class PaymentController {

    @Inject PaymentRepository paymentRepository;
    @Inject MembershipRepository membershipRepository;
    @Inject UserRepository userRepository;
    @Inject CreditService creditService;
    @Inject NotificationService notificationService;
    @Inject JwtAuthUtil jwtAuthUtil;

    private static final List<String> ORG_ROLES =
            List.of("organizer_primary", "organizer_additional", "co_organizer");

    @Post("/mark-paid")
    public HttpResponse<?> markPaid(@Header("Authorization") String auth,
                                     @PathVariable String clubId,
                                     @Body Map<String, Object> body) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            double amount = ((Number) body.get("amount")).doubleValue();
            var membership = membershipRepository.findByUserAndClub(ctx.userId(), clubId)
                    .orElseThrow(() -> RallyhubException.forbidden("Not a member"));
            Payment p = Payment.builder().id(UUID.randomUUID().toString())
                    .userId(ctx.userId()).clubId(clubId).amount(amount)
                    .reference(membership.getPaymentReference()).status("pending")
                    .markedPaidAt(Instant.now()).createdAt(Instant.now()).build();
            paymentRepository.save(p);
            membershipRepository.findByClubId(clubId).stream()
                    .filter(m -> ORG_ROLES.contains(m.getRole()))
                    .forEach(m -> userRepository.findById(m.getUserId()).ifPresent(u ->
                            notificationService.send(u.getPushToken(), u.getEmail(),
                                    "payment_mark_as_paid", "Payment pending",
                                    ctx.email() + " marked £" + amount + " as paid")));
            return HttpResponse.created(p);
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Get("/pending")
    public HttpResponse<?> pending(@Header("Authorization") String auth,
                                    @PathVariable String clubId) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            membershipRepository.findByUserAndClub(ctx.userId(), clubId)
                    .filter(m -> ORG_ROLES.contains(m.getRole()))
                    .orElseThrow(() -> RallyhubException.forbidden("Only organisers can view payments"));
            var items = paymentRepository.findPendingByClub(clubId);
            return HttpResponse.ok(Map.of("items", items, "count", items.size()));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Post("/{paymentId}/verify")
    public HttpResponse<?> verify(@Header("Authorization") String auth,
                                   @PathVariable String clubId,
                                   @PathVariable String paymentId,
                                   @Body Map<String, Object> body) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            membershipRepository.findByUserAndClub(ctx.userId(), clubId)
                    .filter(m -> ORG_ROLES.contains(m.getRole()))
                    .orElseThrow(() -> RallyhubException.forbidden("Only organisers can verify payments"));
            Payment p = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> RallyhubException.notFound("Payment"));
            int credits = ((Number) body.get("creditsToAdd")).intValue();
            p.setStatus("verified"); p.setVerifiedBy(ctx.userId());
            p.setCreditsAdded(credits); p.setVerifiedAt(Instant.now());
            paymentRepository.update(p);
            var m = creditService.topUp(ctx.userId(), clubId, p.getUserId(), credits);
            userRepository.findById(p.getUserId()).ifPresent(u ->
                    notificationService.send(u.getPushToken(), u.getEmail(),
                            "credit_adjusted", "Credits added",
                            credits + " credits added to your balance"));
            return HttpResponse.ok(Map.of("newBalance", m.getCreditBalance()));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    private HttpResponse<?> toResponse(RallyhubException e) {
        return HttpResponse.status(HttpStatus.valueOf(e.getStatus()))
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }
}
