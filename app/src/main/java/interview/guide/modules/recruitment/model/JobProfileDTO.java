package interview.guide.modules.recruitment.model;

import java.time.LocalDateTime;

public record JobProfileDTO(
    Long id,
    Long jobPostId,
    Long jobPostVersionId,
    String hardRequirementsJson,
    String competenciesJson,
    String bonusItemsJson,
    String eliminationItemsJson,
    String verificationPointsJson,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
