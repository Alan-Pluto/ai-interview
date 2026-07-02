package interview.guide.modules.recruitment.model;

import jakarta.validation.constraints.NotBlank;

public record CreateJobPostRequest(
    @NotBlank String title,
    String department,
    String location,
    String employmentType,
    String seniority,
    String initialRequirement
) {}
