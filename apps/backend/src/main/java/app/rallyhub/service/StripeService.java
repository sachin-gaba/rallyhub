package app.rallyhub.service;

import app.rallyhub.domain.model.Club;
import app.rallyhub.domain.repository.ClubRepository;
import app.rallyhub.exception.RallyhubException;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Stripe integration for organizer → platform subscriptions.
 * NOTE: Member-to-club payments are NOT processed here (manual bank transfer only).
 *
 * Uses Stripe's REST API directly (no Stripe SDK — keeps native image simple).
 */
@Slf4j
@Singleton
public class StripeService {

    private static final String STRIPE_API = "https://api.stripe.com/v1";
    private static final Map<String, String> PLAN_PRICE_IDS = Map.of(
            "club",       "price_club_monthly",       // replace with real Stripe Price IDs
            "multi_club", "price_multi_club_monthly"
    );

    @Value("${rallyhub.stripe.secret-key:}")
    private String stripeSecretKey;

    private final ClubRepository clubRepository;
    private final PlanService planService;

    public StripeService(ClubRepository clubRepository, PlanService planService) {
        this.clubRepository = clubRepository;
        this.planService    = planService;
    }

    /**
     * Create a Stripe Checkout Session for an organizer to subscribe.
     * Returns the Stripe Checkout URL to redirect/open in a browser.
     */
    public String createCheckoutSession(String clubId, String plan, String successUrl, String cancelUrl)
            throws Exception {
        String priceId = PLAN_PRICE_IDS.get(plan);
        if (priceId == null) throw RallyhubException.badRequest("Unknown plan: " + plan);

        String body = "mode=subscription"
                + "&line_items[0][price]=" + priceId
                + "&line_items[0][quantity]=1"
                + "&success_url=" + successUrl + "?session_id={CHECKOUT_SESSION_ID}"
                + "&cancel_url=" + cancelUrl
                + "&metadata[clubId]=" + clubId
                + "&metadata[plan]=" + plan;

        String response = stripePost("/checkout/sessions", body);
        // Parse url from response JSON (simple extraction — avoids JSON library dependency)
        String url = extractJsonField(response, "url");
        log.info("Created Stripe checkout session for club {} plan {}", clubId, plan);
        return url;
    }

    /**
     * Handle Stripe webhook: checkout.session.completed
     * Called by the webhook endpoint after Stripe confirms payment.
     */
    public void handleCheckoutCompleted(String webhookPayload, String stripeSignature)
            throws Exception {
        // In production: verify Stripe-Signature header with webhook signing secret
        // For scaffolding: parse clubId + plan from metadata and upgrade
        String clubId = extractJsonField(webhookPayload, "clubId");
        String plan   = extractJsonField(webhookPayload, "plan");
        if (clubId != null && plan != null) {
            planService.upgradePlan(clubId, plan);
            log.info("Stripe webhook: upgraded club {} to {}", clubId, plan);
        }
    }

    /**
     * Cancel a Stripe subscription when organizer downgrades.
     */
    public void cancelSubscription(String stripeSubscriptionId) throws Exception {
        stripeDelete("/subscriptions/" + stripeSubscriptionId);
        log.info("Cancelled Stripe subscription {}", stripeSubscriptionId);
    }

    // ── Low-level HTTP helpers (native-image friendly) ───────────

    private String stripePost(String path, String formBody) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(STRIPE_API + path))
                .header("Authorization", "Bearer " + stripeSecretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private String stripeDelete(String path) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(STRIPE_API + path))
                .header("Authorization", "Bearer " + stripeSecretKey)
                .DELETE().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }
}
