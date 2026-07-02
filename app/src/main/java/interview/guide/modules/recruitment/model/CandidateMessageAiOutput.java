package interview.guide.modules.recruitment.model;

import java.util.List;

public record CandidateMessageAiOutput(
    String subject,
    String content,
    List<String> keyPoints,
    List<String> cautionNotes
) {}
