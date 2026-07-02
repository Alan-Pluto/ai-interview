package interview.guide.modules.recruitment.model;

import java.util.List;

public final class JobAiOutputs {

  private JobAiOutputs() {}

  public record JobDraftOutput(
      String title,
      String department,
      String location,
      String employmentType,
      String seniority,
      String jdContent,
      StructuredJobContent structuredContent,
      String changeSummary,
      String assistantReply
  ) {}

  public record StructuredJobContent(
      List<String> responsibilities,
      List<String> requirements,
      List<String> bonusItems,
      List<String> benefits,
      List<String> highlights
  ) {}

  public record ComplianceReviewOutput(
      String overallRiskLevel,
      Integer riskScore,
      List<RiskItemOutput> risks
  ) {}

  public record RiskItemOutput(
      String riskLevel,
      String riskType,
      String matchedText,
      String description,
      String suggestion,
      String basis
  ) {}

  public record JobProfileOutput(
      List<String> hardRequirements,
      List<String> competencies,
      List<String> bonusItems,
      List<String> eliminationItems,
      List<String> verificationPoints
  ) {}
}
