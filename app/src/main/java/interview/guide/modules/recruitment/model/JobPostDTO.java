package interview.guide.modules.recruitment.model;

import java.time.LocalDateTime;

public record JobPostDTO(
    Long id,
    String title,
    String department,
    String location,
    String employmentType,
    String seniority,
    Long currentVersionId,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
