package app.rallyhub.controller;

import app.rallyhub.domain.model.Announcement;
import app.rallyhub.domain.repository.AnnouncementRepository;
import app.rallyhub.domain.repository.MembershipRepository;
import app.rallyhub.exception.RallyhubException;
import app.rallyhub.util.JwtAuthUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.*;

@Controller("/v1/clubs/{clubId}/announcements")
public class AnnouncementController {

    @Inject AnnouncementRepository announcementRepository;
    @Inject MembershipRepository membershipRepository;
    @Inject JwtAuthUtil jwtAuthUtil;

    private static final List<String> ORG_ROLES =
            List.of("organizer_primary", "organizer_additional", "co_organizer");

    @Get
    public HttpResponse<?> list(@Header("Authorization") String auth,
                                 @PathVariable String clubId) {
        try {
            jwtAuthUtil.verify(auth);
            var items = announcementRepository.findByClubId(clubId);
            return HttpResponse.ok(Map.of("items", items, "count", items.size()));
        } catch (RallyhubException e) { return toResponse(e); }
    }

    @Post
    public HttpResponse<?> create(@Header("Authorization") String auth,
                                   @PathVariable String clubId,
                                   @Body Map<String, Object> body) {
        try {
            var ctx = jwtAuthUtil.verify(auth);
            membershipRepository.findByUserAndClub(ctx.userId(), clubId)
                    .filter(m -> ORG_ROLES.contains(m.getRole()))
                    .orElseThrow(() -> RallyhubException.forbidden("Only organisers can post announcements"));
            String text    = (String) body.get("body");
            boolean pinned = Boolean.TRUE.equals(body.get("pinned"));
            if (text == null || text.isBlank())
                return HttpResponse.badRequest(Map.of("code", "VALIDATION", "message", "body is required"));
            var a = Announcement.builder().id(UUID.randomUUID().toString())
                    .clubId(clubId).authorId(ctx.userId()).body(text).pinned(pinned)
                    .createdAt(Instant.now()).updatedAt(Instant.now()).build();
            announcementRepository.save(a);
            return HttpResponse.created(a);
        } catch (RallyhubException e) { return toResponse(e); }
    }

    private HttpResponse<?> toResponse(RallyhubException e) {
        return HttpResponse.status(HttpStatus.valueOf(e.getStatus()))
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }
}
