package interview.guide.modules.recruitment.model;

import jakarta.validation.constraints.NotBlank;

public record OptimizeCandidateMessageDraftRequest(
    @NotBlank String instruction,
    String provider
) {}
