package interview.guide.modules.recruitment.model;

import java.util.List;

public record JobPostDetailDTO(
    JobPostDTO jobPost,
    JobPostVersionDTO currentVersion,
    List<ComplianceRiskItemDTO> complianceRisks,
    JobProfileDTO jobProfile
) {}
