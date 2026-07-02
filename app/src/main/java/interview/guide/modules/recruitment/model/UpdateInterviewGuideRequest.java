package interview.guide.modules.recruitment.model;

import jakarta.validation.constraints.NotBlank;

public record UpdateInterviewGuideRequest(
    @NotBlank String outlineJson,
    @NotBlank String scorecardJson
) {}
