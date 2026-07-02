package interview.guide.modules.recruitment.model;

import java.time.LocalDateTime;

public record CandidateJobMatchDTO(
    Long id,
    Long jobPostId,
    Long jobPostVersionId,
    Long resumeId,
    String resumeFilename,
    String batchId,
    Integer matchScore,
    String recommendation,
    String summary,
    String strengthsJson,
    String risksJson,
    String requirementChecksJson,
    String verificationQuestionsJson,
    String status,
    LocalDateTime createdAt
) {}
