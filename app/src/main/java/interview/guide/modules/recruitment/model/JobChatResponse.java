package interview.guide.modules.recruitment.model;

import java.util.List;

public record JobChatResponse(
    JobPostDTO jobPost,
    JobConversationDTO conversation,
    JobConversationMessageDTO assistantMessage,
    JobPostVersionDTO version,
    List<ComplianceRiskItemDTO> complianceRisks,
    JobProfileDTO jobProfile
) {}
