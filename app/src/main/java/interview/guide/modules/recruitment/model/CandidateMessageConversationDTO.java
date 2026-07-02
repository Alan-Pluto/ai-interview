package interview.guide.modules.recruitment.model;

import java.time.LocalDateTime;

public record CandidateMessageConversationDTO(
    Long id,
    Long draftId,
    String title,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
