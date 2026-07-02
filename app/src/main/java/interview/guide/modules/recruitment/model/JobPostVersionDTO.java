package interview.guide.modules.recruitment.model;

import java.time.LocalDateTime;

public record JobPostVersionDTO(
    Long id,
    Long jobPostId,
    Integer versionNo,
    String content,
    String structuredContentJson,
    String changeSummary,
    Boolean createdByAi,
    LocalDateTime createdAt
) {}
