package interview.guide.modules.recruitment.model;

public record ComplianceRiskItemDTO(
    Long id,
    Long jobPostVersionId,
    String riskLevel,
    String riskType,
    String matchedText,
    String description,
    String suggestion,
    String basis,
    Integer sortOrder
) {}
