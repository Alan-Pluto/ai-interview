package interview.guide.modules.recruitment.model;

import java.time.LocalDateTime;

public record InterviewGuideDTO(
    Long id,
    Long jobPostId,
    Long resumeId,
    String resumeFilename,
    Long matchId,
    String interviewRound,
    Integer durationMinutes,
    String mode,
    String outlineJson,
    String scorecardJson,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
