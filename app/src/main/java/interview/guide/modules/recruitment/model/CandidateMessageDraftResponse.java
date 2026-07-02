package interview.guide.modules.recruitment.model;

public record CandidateMessageDraftResponse(
    CandidateMessageDraftDTO draft,
    CandidateMessageConversationDTO conversation,
    CandidateMessageConversationMessageDTO assistantMessage
) {}
