package interview.guide.modules.recruitment.model;

import java.util.List;

public record InterviewGuideAiOutput(
    List<OutlineSection> outline,
    List<ScoreDimension> scorecard
) {

  public record OutlineSection(
      String section,
      Integer suggestedMinutes,
      List<QuestionItem> questions
  ) {}

  public record QuestionItem(
      String question,
      String purpose,
      String evaluationFocus,
      String excellentSignal,
      String riskSignal
  ) {}

  public record ScoreDimension(
      String dimension,
      Integer weight,
      String description,
      List<String> scoringCriteria
  ) {}
}
