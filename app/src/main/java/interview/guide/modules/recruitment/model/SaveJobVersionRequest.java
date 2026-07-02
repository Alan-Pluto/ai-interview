package interview.guide.modules.recruitment.model;

import jakarta.validation.constraints.NotBlank;

public record SaveJobVersionRequest(
    @NotBlank String content,
    String structuredContentJson,
    String changeSummary
) {}
