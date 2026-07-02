package interview.guide.modules.recruitment.model;

import java.time.LocalDateTime;

public record JobConversationMessageDTO(
    Long id,
    Long conversationId,
    String role,
    String content,
    Long relatedVersionId,
    Integer messageOrder,
    LocalDateTime createdAt
) {}
