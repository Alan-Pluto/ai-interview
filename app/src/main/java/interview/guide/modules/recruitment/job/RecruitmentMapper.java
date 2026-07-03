package interview.guide.modules.recruitment.job;

import interview.guide.modules.recruitment.model.ComplianceRiskItemDTO;
import interview.guide.modules.recruitment.model.ComplianceRiskItemEntity;
import interview.guide.modules.recruitment.model.CandidateJobMatchDTO;
import interview.guide.modules.recruitment.model.CandidateJobMatchEntity;
import interview.guide.modules.recruitment.model.CandidateMessageConversationDTO;
import interview.guide.modules.recruitment.model.CandidateMessageConversationEntity;
import interview.guide.modules.recruitment.model.CandidateMessageConversationMessageDTO;
import interview.guide.modules.recruitment.model.CandidateMessageConversationMessageEntity;
import interview.guide.modules.recruitment.model.CandidateMessageDraftDTO;
import interview.guide.modules.recruitment.model.CandidateMessageDraftEntity;
import interview.guide.modules.recruitment.model.JobConversationDTO;
import interview.guide.modules.recruitment.model.JobConversationEntity;
import interview.guide.modules.recruitment.model.JobConversationMessageDTO;
import interview.guide.modules.recruitment.model.JobConversationMessageEntity;
import interview.guide.modules.recruitment.model.InterviewGuideDTO;
import interview.guide.modules.recruitment.model.InterviewGuideEntity;
import interview.guide.modules.recruitment.model.JobPostDTO;
import interview.guide.modules.recruitment.model.JobPostEntity;
import interview.guide.modules.recruitment.model.JobPostVersionDTO;
import interview.guide.modules.recruitment.model.JobPostVersionEntity;
import interview.guide.modules.recruitment.model.JobProfileDTO;
import interview.guide.modules.recruitment.model.JobProfileEntity;
import org.springframework.stereotype.Component;

@Component
public class RecruitmentMapper {

  public JobPostDTO toJobPostDTO(JobPostEntity entity) {
    if (entity == null) {
      return null;
    }
    return new JobPostDTO(
        entity.getId(),
        entity.getTitle(),
        entity.getDepartment(),
        entity.getLocation(),
        entity.getEmploymentType(),
        entity.getSeniority(),
        entity.getCurrentVersionId(),
        entity.getStatus().name(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public JobPostVersionDTO toVersionDTO(JobPostVersionEntity entity) {
    if (entity == null) {
      return null;
    }
    return new JobPostVersionDTO(
        entity.getId(),
        entity.getJobPost().getId(),
        entity.getVersionNo(),
        entity.getContent(),
        entity.getStructuredContentJson(),
        entity.getChangeSummary(),
        entity.getCreatedByAi(),
        entity.getCreatedAt()
    );
  }

  public JobConversationDTO toConversationDTO(JobConversationEntity entity) {
    if (entity == null) {
      return null;
    }
    return new JobConversationDTO(
        entity.getId(),
        entity.getJobPost().getId(),
        entity.getTitle(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public JobConversationMessageDTO toMessageDTO(JobConversationMessageEntity entity) {
    if (entity == null) {
      return null;
    }
    return new JobConversationMessageDTO(
        entity.getId(),
        entity.getConversation().getId(),
        entity.getRole().name().toLowerCase(),
        entity.getContent(),
        entity.getRelatedVersionId(),
        entity.getMessageOrder(),
        entity.getCreatedAt()
    );
  }

  public ComplianceRiskItemDTO toRiskDTO(ComplianceRiskItemEntity entity) {
    if (entity == null) {
      return null;
    }
    return new ComplianceRiskItemDTO(
        entity.getId(),
        entity.getJobPostVersion().getId(),
        entity.getRiskLevel().name(),
        entity.getRiskType(),
        entity.getMatchedText(),
        entity.getDescription(),
        entity.getSuggestion(),
        entity.getBasis(),
        entity.getSortOrder()
    );
  }

  public JobProfileDTO toProfileDTO(JobProfileEntity entity) {
    if (entity == null) {
      return null;
    }
    return new JobProfileDTO(
        entity.getId(),
        entity.getJobPost().getId(),
        entity.getJobPostVersion().getId(),
        entity.getHardRequirementsJson(),
        entity.getCompetenciesJson(),
        entity.getBonusItemsJson(),
        entity.getEliminationItemsJson(),
        entity.getVerificationPointsJson(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public CandidateJobMatchDTO toMatchDTO(CandidateJobMatchEntity entity) {
    if (entity == null) {
      return null;
    }
    return new CandidateJobMatchDTO(
        entity.getId(),
        entity.getJobPost().getId(),
        entity.getJobPost().getTitle(),
        entity.getJobPostVersion().getId(),
        entity.getResume().getId(),
        entity.getResume().getOriginalFilename(),
        entity.getBatchId(),
        entity.getMatchScore(),
        entity.getRecommendation().name(),
        entity.getSummary(),
        entity.getStrengthsJson(),
        entity.getRisksJson(),
        entity.getRequirementChecksJson(),
        entity.getVerificationQuestionsJson(),
        entity.getStatus(),
        entity.getCreatedAt()
    );
  }

  public InterviewGuideDTO toInterviewGuideDTO(InterviewGuideEntity entity) {
    if (entity == null) {
      return null;
    }
    return new InterviewGuideDTO(
        entity.getId(),
        entity.getJobPost().getId(),
        entity.getJobPost().getTitle(),
        entity.getResume() == null ? null : entity.getResume().getId(),
        entity.getResume() == null ? null : entity.getResume().getOriginalFilename(),
        entity.getMatch() == null ? null : entity.getMatch().getId(),
        entity.getInterviewRound(),
        entity.getDurationMinutes(),
        entity.getMode().name(),
        entity.getOutlineJson(),
        entity.getScorecardJson(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public CandidateMessageDraftDTO toCandidateMessageDraftDTO(CandidateMessageDraftEntity entity) {
    if (entity == null) {
      return null;
    }
    return new CandidateMessageDraftDTO(
        entity.getId(),
        entity.getJobPost().getId(),
        entity.getJobPost().getTitle(),
        entity.getResume() == null ? null : entity.getResume().getId(),
        entity.getResume() == null ? null : entity.getResume().getOriginalFilename(),
        entity.getMatch() == null ? null : entity.getMatch().getId(),
        entity.getInterviewGuide() == null ? null : entity.getInterviewGuide().getId(),
        entity.getMessageType(),
        entity.getChannel(),
        entity.getStage(),
        entity.getTone(),
        entity.getSubject(),
        entity.getContent(),
        entity.getKeyPointsJson(),
        entity.getCautionNotesJson(),
        entity.getStatus().name(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public CandidateMessageConversationDTO toCandidateMessageConversationDTO(
      CandidateMessageConversationEntity entity) {
    if (entity == null) {
      return null;
    }
    return new CandidateMessageConversationDTO(
        entity.getId(),
        entity.getDraft().getId(),
        entity.getTitle(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public CandidateMessageConversationMessageDTO toCandidateMessageMessageDTO(
      CandidateMessageConversationMessageEntity entity) {
    if (entity == null) {
      return null;
    }
    return new CandidateMessageConversationMessageDTO(
        entity.getId(),
        entity.getConversation().getId(),
        entity.getRole().name().toLowerCase(),
        entity.getContent(),
        entity.getMessageOrder(),
        entity.getCreatedAt()
    );
  }
}
