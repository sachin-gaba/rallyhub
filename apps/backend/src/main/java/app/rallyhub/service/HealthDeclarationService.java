import jakarta.inject.Singleton;
package app.rallyhub.service;

import app.rallyhub.domain.model.*;
import app.rallyhub.domain.repository.*;
import app.rallyhub.exception.RallyhubException;
import lombok.RequiredArgsConstructor;


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor
public class HealthDeclarationService {

    private final HealthDeclarationRepository declarationRepository;
    private final MembershipRepository membershipRepository;

    public HealthDeclaration submit(String userId, String clubId,
                                    Map<Integer, Boolean> parqAnswers,
                                    List<String> medicalConditions,
                                    String medications,
                                    HealthDeclaration.EmergencyContact emergencyContact,
                                    boolean liabilityAccepted,
                                    boolean dataProtectionConsented,
                                    String deviceInfo,
                                    String appVersion) {
        if (!liabilityAccepted || !dataProtectionConsented)
            throw RallyhubException.badRequest("Liability waiver and data consent are required");

        if (emergencyContact == null || emergencyContact.getFullName() == null
                || emergencyContact.getPrimaryPhone() == null)
            throw RallyhubException.badRequest("Emergency contact name and phone are required");

        HealthDeclaration declaration = HealthDeclaration.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .clubId(clubId)
                .parqAnswers(parqAnswers)
                .medicalConditions(medicalConditions)
                .medications(medications)
                .emergencyContact(emergencyContact)
                .liabilityAccepted(true)
                .dataProtectionConsented(true)
                .submittedAt(Instant.now())
                .deviceInfo(deviceInfo)
                .appVersion(appVersion)
                .build();

        declarationRepository.save(declaration);

        // Update membership record
        membershipRepository.findByUserAndClub(userId, clubId).ifPresent(m -> {
            m.setHealthDeclarationSubmitted(true);
            m.setHealthDeclarationId(declaration.getId());
            membershipRepository.update(m);
        });

        // TODO: generate PDF, email to member, notify organizer
        return declaration;
    }

    public ClubMembership promoteToFullMember(String actorId, String clubId, String targetUserId) {
        // Verify actor has promotion rights (organizer or co_organizer)
        membershipRepository.findByUserAndClub(actorId, clubId)
                .filter(m -> m.getRole().startsWith("organizer") || "co_organizer".equals(m.getRole()))
                .orElseThrow(() -> RallyhubException.forbidden("Only organisers can promote members"));

        ClubMembership target = membershipRepository.findByUserAndClub(targetUserId, clubId)
                .orElseThrow(() -> RallyhubException.notFound("Member"));

        if (!target.isHealthDeclarationSubmitted())
            throw RallyhubException.badRequest(
                    "Health declaration not yet submitted. The member must complete this before promotion.");

        target.setRole("full_member");
        membershipRepository.update(target);
        return target;
    }
}
