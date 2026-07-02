package interview.guide.modules.recruitment.model;

import java.time.LocalDateTime;

public record JobConversationDTO(
    Long id,
    Long jobPostId,
    String title,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
