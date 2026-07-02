package interview.guide.modules.recruitment.model;

import java.time.LocalDateTime;

public record CandidateMessageConversationMessageDTO(
    Long id,
    Long conversationId,
    String role,
    String content,
    Integer messageOrder,
    LocalDateTime createdAt
) {}
