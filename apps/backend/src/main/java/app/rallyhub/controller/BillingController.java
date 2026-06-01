package app.rallyhub.controller;

import app.rallyhub.domain.repository.MembershipRepository;
import app.rallyhub.exception.RallyhubException;
import app.rallyhub.service.PlanService;
import app.rallyhub.service.StripeService;
import app.rallyhub.util.JwtAuthUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;

import java.util.Map;

@Controller("/v1")
public class BillingController {

    @Inject StripeService stripeService;
    @Inject PlanService planService;
    @Inject MembershipRepository membershipRepository;
    @Inject JwtAuthUtil jwtAuthUtil;

    private static final String ORGANIZER_PRIMARY = "organizer_primary";

    /** POST /v1/clubs/{clubId}/billing/checkout — start Stripe checkout */
    @Post("/clubs/{clubId}/billing/checkout")
    public HttpResponse<?> startCheckout(@Header("Authorization") String auth,
                                          @PathVariable String clubId,
                                          @Body Map<String, Object> body) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            membershipRepository.findByUserAndClub(ctx.userId(), clubId)
                    .filter(m -> ORGANIZER_PRIMARY.equals(m.getRole()))
                    .orElseThrow(() -> RallyhubException.forbidden("Only the Primary Organiser can manage billing"));

            String plan       = (String) body.get("plan");
            String successUrl = (String) body.getOrDefault("successUrl", "https://rallyhub.app/billing/success");
            String cancelUrl  = (String) body.getOrDefault("cancelUrl",  "https://rallyhub.app/billing/cancel");

            String checkoutUrl = stripeService.createCheckoutSession(clubId, plan, successUrl, cancelUrl);
            return HttpResponse.ok(Map.of("checkoutUrl", checkoutUrl));
        } catch (RallyhubException e) {
            return HttpResponse.status(HttpStatus.valueOf(e.getStatus()))
                    .body(Map.of("code", e.getCode(), "message", e.getMessage()));
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("code", "STRIPE_ERROR", "message", e.getMessage()));
        }
    }

    /** POST /v1/stripe/webhook — Stripe webhook receiver */
    @Post("/stripe/webhook")
    public HttpResponse<?> stripeWebhook(@Header("Stripe-Signature") String signature,
                                          @Body String payload) {
        try {
            stripeService.handleCheckoutCompleted(payload, signature);
            return HttpResponse.ok(Map.of("received", true));
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("error", e.getMessage()));
        }
    }

    /** GET /v1/clubs/{clubId}/plan */
    @Get("/clubs/{clubId}/plan")
    public HttpResponse<?> getPlan(@Header("Authorization") String auth,
                                    @PathVariable String clubId) {
        try {
            jwtAuthUtil.verify(auth);
            // Plan info is on the Club object — return it
            return HttpResponse.ok(Map.of("clubId", clubId));
        } catch (RallyhubException e) {
            return HttpResponse.status(HttpStatus.valueOf(e.getStatus()))
                    .body(Map.of("code", e.getCode(), "message", e.getMessage()));
        }
    }
}
