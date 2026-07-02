package interview.guide.modules.recruitment.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCandidateMessageDraftRequest(
    @NotNull Long jobPostId,
    Long resumeId,
    Long matchId,
    Long interviewGuideId,
    @NotBlank String messageType,
    @NotBlank String channel,
    @NotBlank String stage,
    @NotBlank String tone,
    String extraInstruction,
    String provider
) {}
