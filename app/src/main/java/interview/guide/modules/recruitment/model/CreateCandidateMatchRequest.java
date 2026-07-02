package interview.guide.modules.recruitment.model;

import jakarta.validation.constraints.NotNull;

public record CreateCandidateMatchRequest(
    @NotNull Long resumeId,
    String provider
) {}
