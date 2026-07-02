package interview.guide.modules.recruitment.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInterviewGuideRequest(
    @NotNull Long jobPostId,
    Long matchId,
    @NotBlank String mode,
    @NotBlank String interviewRound,
    Integer durationMinutes,
    String provider
) {}
