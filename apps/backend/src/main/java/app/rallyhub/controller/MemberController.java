package app.rallyhub.controller;

import app.rallyhub.domain.model.*;
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

@Controller("/v1/clubs/{clubId}")
public class MemberController {

    @Inject MembershipRepository membershipRepository;
    @Inject JoinRequestRepository joinRequestRepository;
    @Inject UserRepository userRepository;
    @Inject NotificationService notificationService;
    @Inject HealthDeclarationService healthDeclarationService;
    @Inject JwtAuthUtil jwtAuthUtil;

    private static final List<String> ORG_ROLES =
            List.of("organizer_primary", "organizer_additional", "co_organizer");

    @Get("/membership/me")
    public HttpResponse<?> getMyMembership(@Header("Authorization") String auth,
                                            @PathVariable String clubId) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            var m = membershipRepository.findByUserAndClub(ctx.userId(), clubId)
                    .orElseThrow(() -> RallyhubException.notFound("Membership"));
            return HttpResponse.ok(m);
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Get("/members")
    public HttpResponse<?> listMembers(@Header("Authorization") String auth,
                                        @PathVariable String clubId) {
        try {
            jwtAuthUtil.verify(auth);
            var members = membershipRepository.findByClubId(clubId);
            return HttpResponse.ok(Map.of("items", members, "count", members.size()));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Post("/join")
    public HttpResponse<?> joinRequest(@Header("Authorization") String auth,
                                        @PathVariable String clubId) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            if (membershipRepository.findByUserAndClub(ctx.userId(), clubId).isPresent())
                return HttpResponse.status(HttpStatus.CONFLICT)
                        .body(Map.of("code", "CONFLICT", "message", "Already a member or request pending"));

            JoinRequest req = JoinRequest.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(ctx.userId()).clubId(clubId)
                    .status("pending").requestedAt(Instant.now()).build();
            joinRequestRepository.save(req);

            membershipRepository.findByClubId(clubId).stream()
                    .filter(m -> ORG_ROLES.contains(m.getRole()))
                    .forEach(m -> userRepository.findById(m.getUserId()).ifPresent(u ->
                            notificationService.send(u.getPushToken(), u.getEmail(),
                                    "new_join_request", "New join request",
                                    ctx.email() + " wants to join your club")));
            return HttpResponse.created(req);
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Get("/join-requests")
    public HttpResponse<?> listJoinRequests(@Header("Authorization") String auth,
                                             @PathVariable String clubId) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            membershipRepository.findByUserAndClub(ctx.userId(), clubId)
                    .filter(m -> ORG_ROLES.contains(m.getRole()))
                    .orElseThrow(() -> RallyhubException.forbidden("Only organisers can view join requests"));
            var items = joinRequestRepository.findPendingByClub(clubId);
            return HttpResponse.ok(Map.of("items", items, "count", items.size()));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Patch("/join-requests/{requestId}/review")
    public HttpResponse<?> reviewJoinRequest(@Header("Authorization") String auth,
                                              @PathVariable String clubId,
                                              @PathVariable String requestId,
                                              @Body Map<String, Object> body) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            membershipRepository.findByUserAndClub(ctx.userId(), clubId)
                    .filter(m -> ORG_ROLES.contains(m.getRole()))
                    .orElseThrow(() -> RallyhubException.forbidden("Only organisers can review join requests"));

            JoinRequest req = joinRequestRepository.findById(requestId)
                    .orElseThrow(() -> RallyhubException.notFound("Join request"));
            String decision = (String) body.get("decision");
            String note     = (String) body.getOrDefault("note", "");
            req.setStatus(decision); req.setReviewedBy(ctx.userId());
            req.setReviewNote(note); req.setReviewedAt(Instant.now());
            joinRequestRepository.update(req);

            if ("accepted".equals(decision)) {
                    planService.enforceMemberCap(clubId);
                membershipRepository.save(ClubMembership.builder()
                        .userId(req.getUserId()).clubId(clubId).role("inductee")
                        .creditBalance(0).paymentReference(clubId.substring(0, 4).toUpperCase() + "-" + req.getUserId().substring(0, 4).toUpperCase())
                        .joinedAt(Instant.now()).inductionCompleted(false).healthDeclarationSubmitted(false).build());
            }
            userRepository.findById(req.getUserId()).ifPresent(u ->
                    notificationService.send(u.getPushToken(), u.getEmail(),
                            "join_request_" + decision, "Join request " + decision,
                            "Your request to join the club has been " + decision));
            return HttpResponse.ok(req);
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Post("/health-declaration")
    public HttpResponse<?> submitHealthDeclaration(@Header("Authorization") String auth,
                                                    @PathVariable String clubId,
                                                    @Body Map<String, Object> body) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            var ecMap = (Map<?, ?>) body.get("emergencyContact");
            var ec = HealthDeclaration.EmergencyContact.builder()
                    .fullName((String) ecMap.get("fullName"))
                    .relationship((String) ecMap.get("relationship"))
                    .primaryPhone((String) ecMap.get("primaryPhone"))
                    .secondaryPhone((String) ecMap.get("secondaryPhone")).build();
            var decl = healthDeclarationService.submit(
                    ctx.userId(), clubId,
                    (Map<Integer, Boolean>) body.get("parqAnswers"),
                    (List<String>) body.get("medicalConditions"),
                    (String) body.getOrDefault("medications", ""), ec,
                    Boolean.TRUE.equals(body.get("liabilityAccepted")),
                    Boolean.TRUE.equals(body.get("dataProtectionConsented")),
                    "", "");
            return HttpResponse.created(decl);
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Patch("/members/{userId}/promote")
    public HttpResponse<?> promote(@Header("Authorization") String auth,
                                    @PathVariable String clubId,
                                    @PathVariable String userId) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            var m = healthDeclarationService.promoteToFullMember(ctx.userId(), clubId, userId);
            return HttpResponse.ok(Map.of("role", m.getRole()));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    private HttpResponse<?> toResponse(RallyhubException e) {
        return HttpResponse.status(HttpStatus.valueOf(e.getStatus()))
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }
}
