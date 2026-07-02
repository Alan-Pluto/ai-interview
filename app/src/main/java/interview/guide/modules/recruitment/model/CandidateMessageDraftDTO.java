package interview.guide.modules.recruitment.model;

import java.time.LocalDateTime;

public record CandidateMessageDraftDTO(
    Long id,
    Long jobPostId,
    Long resumeId,
    String resumeFilename,
    Long matchId,
    Long interviewGuideId,
    String messageType,
    String channel,
    String stage,
    String tone,
    String subject,
    String content,
    String keyPointsJson,
    String cautionNotesJson,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
