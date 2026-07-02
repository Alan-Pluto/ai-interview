package interview.guide.modules.recruitment.model;

import jakarta.validation.constraints.NotBlank;

public record UpdateJobPostRequest(
    @NotBlank String title,
    String department,
    String location,
    String employmentType,
    String seniority
) {}
