package interview.guide.modules.recruitment.model;

import java.util.List;

public record CandidateMatchAiOutput(
    Integer matchScore,
    String recommendation,
    String summary,
    List<String> strengths,
    List<String> risks,
    List<RequirementCheck> requirementChecks,
    List<String> verificationQuestions
) {

  public record RequirementCheck(
      String requirement,
      Boolean satisfied,
      String evidence,
      String confidence
  ) {}
}
