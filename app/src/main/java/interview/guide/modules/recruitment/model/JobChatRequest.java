package interview.guide.modules.recruitment.model;

import jakarta.validation.constraints.NotBlank;

public record JobChatRequest(
    Long conversationId,
    @NotBlank String message,
    String provider
) {}
