package interview.guide.modules.recruitment.model;

import jakarta.validation.constraints.NotBlank;

public record UpdateCandidateMessageDraftRequest(
    String subject,
    @NotBlank String content
) {}
