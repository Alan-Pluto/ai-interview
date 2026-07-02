package interview.guide.modules.recruitment.job;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.transaction.TransactionalExecutor;
import interview.guide.modules.recruitment.model.ComplianceRiskItemDTO;
import interview.guide.modules.recruitment.model.ComplianceRiskItemEntity;
import interview.guide.modules.recruitment.model.ComplianceRiskItemEntity.RiskLevel;
import interview.guide.modules.recruitment.model.CreateJobPostRequest;
import interview.guide.modules.recruitment.model.JobAiOutputs.ComplianceReviewOutput;
import interview.guide.modules.recruitment.model.JobAiOutputs.JobDraftOutput;
import interview.guide.modules.recruitment.model.JobAiOutputs.JobProfileOutput;
import interview.guide.modules.recruitment.model.JobAiOutputs.RiskItemOutput;
import interview.guide.modules.recruitment.model.JobChatRequest;
import interview.guide.modules.recruitment.model.JobChatResponse;
import interview.guide.modules.recruitment.model.JobConversationDTO;
import interview.guide.modules.recruitment.model.JobConversationEntity;
import interview.guide.modules.recruitment.model.JobConversationMessageDTO;
import interview.guide.modules.recruitment.model.JobConversationMessageEntity;
import interview.guide.modules.recruitment.model.JobConversationMessageEntity.MessageRole;
import interview.guide.modules.recruitment.model.JobPostDTO;
import interview.guide.modules.recruitment.model.JobPostDetailDTO;
import interview.guide.modules.recruitment.model.JobPostEntity;
import interview.guide.modules.recruitment.model.JobPostEntity.JobPostStatus;
import interview.guide.modules.recruitment.model.JobPostVersionDTO;
import interview.guide.modules.recruitment.model.JobPostVersionEntity;
import interview.guide.modules.recruitment.model.JobProfileDTO;
import interview.guide.modules.recruitment.model.JobProfileEntity;
import interview.guide.modules.recruitment.model.SaveJobVersionRequest;
import interview.guide.modules.recruitment.model.UpdateJobPostRequest;
import interview.guide.modules.recruitment.repository.ComplianceRiskItemRepository;
import interview.guide.modules.recruitment.repository.JobConversationMessageRepository;
import interview.guide.modules.recruitment.repository.JobConversationRepository;
import interview.guide.modules.recruitment.repository.JobPostRepository;
import interview.guide.modules.recruitment.repository.JobPostVersionRepository;
import interview.guide.modules.recruitment.repository.JobProfileRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobPostService {

  private static final int MIN_MESSAGE_LENGTH = 4;

  private final JobPostRepository jobPostRepository;
  private final JobPostVersionRepository versionRepository;
  private final JobConversationRepository conversationRepository;
  private final JobConversationMessageRepository messageRepository;
  private final ComplianceRiskItemRepository riskRepository;
  private final JobProfileRepository profileRepository;
  private final JobAiService jobAiService;
  private final RecruitmentMapper mapper;
  private final ObjectMapper objectMapper;
  private final TransactionalExecutor transactionalExecutor;

  @Transactional
  public JobPostDTO createJob(CreateJobPostRequest request) {
    JobPostEntity jobPost = new JobPostEntity();
    jobPost.setTitle(request.title().trim());
    jobPost.setDepartment(blankToNull(request.department()));
    jobPost.setLocation(blankToNull(request.location()));
    jobPost.setEmploymentType(blankToNull(request.employmentType()));
    jobPost.setSeniority(blankToNull(request.seniority()));
    JobPostEntity saved = jobPostRepository.save(jobPost);

    if (request.initialRequirement() != null && !request.initialRequirement().isBlank()) {
      JobConversationEntity conversation = createConversation(saved);
      saveMessage(conversation, MessageRole.USER, request.initialRequirement(), null);
    }
    return mapper.toJobPostDTO(saved);
  }

  @Transactional(readOnly = true)
  public List<JobPostDTO> listJobs() {
    return jobPostRepository.findAllByOrderByUpdatedAtDesc()
        .stream()
        .filter(job -> job.getStatus() != JobPostStatus.ARCHIVED)
        .map(mapper::toJobPostDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public JobPostDetailDTO getJobDetail(Long jobId) {
    JobPostEntity job = findJob(jobId);
    JobPostVersionEntity currentVersion = findCurrentVersion(job);
    List<ComplianceRiskItemDTO> risks = currentVersion == null
        ? List.of()
        : riskRepository.findByJobPostVersionOrderBySortOrderAsc(currentVersion)
            .stream()
            .map(mapper::toRiskDTO)
            .toList();
    JobProfileDTO profile = profileRepository.findByJobPost(job)
        .map(mapper::toProfileDTO)
        .orElse(null);
    return new JobPostDetailDTO(
        mapper.toJobPostDTO(job),
        mapper.toVersionDTO(currentVersion),
        risks,
        profile
    );
  }

  @Transactional
  public JobPostDTO updateJob(Long jobId, UpdateJobPostRequest request) {
    JobPostEntity job = findJob(jobId);
    job.setTitle(request.title().trim());
    job.setDepartment(blankToNull(request.department()));
    job.setLocation(blankToNull(request.location()));
    job.setEmploymentType(blankToNull(request.employmentType()));
    job.setSeniority(blankToNull(request.seniority()));
    return mapper.toJobPostDTO(job);
  }

  @Transactional
  public void archiveJob(Long jobId) {
    JobPostEntity job = findJob(jobId);
    job.setStatus(JobPostStatus.ARCHIVED);
  }

  public JobChatResponse chat(Long jobId, JobChatRequest request) {
    if (request.message() == null || request.message().trim().length() < MIN_MESSAGE_LENGTH) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入更完整的 JD 需求或修改意见");
    }

    JobDraftPreparation preparation = transactionalExecutor.call(() -> prepareChat(jobId, request));
    JobDraftOutput draftOutput = jobAiService.generateJobDraft(
        preparation.context(), request.provider());
    if (draftOutput.jdContent() == null || draftOutput.jdContent().isBlank()) {
      throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "JD 生成结果为空，请重试");
    }
    ComplianceReviewOutput complianceOutput = jobAiService.reviewCompliance(
        draftOutput.jdContent(), request.provider());
    JobProfileOutput profileOutput = jobAiService.extractProfile(
        draftOutput.jdContent(), request.provider());

    return transactionalExecutor.call(() ->
        saveChatResult(preparation, request.message(), draftOutput, complianceOutput, profileOutput));
  }

  @Transactional
  public JobPostVersionDTO saveManualVersion(Long jobId, SaveJobVersionRequest request) {
    JobPostEntity job = findJob(jobId);
    JobPostVersionEntity version = createVersion(
        job,
        request.content(),
        request.structuredContentJson(),
        request.changeSummary(),
        false
    );
    job.setCurrentVersionId(version.getId());
    return mapper.toVersionDTO(version);
  }

  @Transactional(readOnly = true)
  public List<JobPostVersionDTO> listVersions(Long jobId) {
    JobPostEntity job = findJob(jobId);
    return versionRepository.findByJobPostOrderByVersionNoDesc(job)
        .stream()
        .map(mapper::toVersionDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<JobConversationDTO> listConversations(Long jobId) {
    JobPostEntity job = findJob(jobId);
    return conversationRepository.findByJobPostOrderByUpdatedAtDesc(job)
        .stream()
        .map(mapper::toConversationDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<JobConversationMessageDTO> listMessages(Long conversationId) {
    JobConversationEntity conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "JD 对话不存在"));
    return messageRepository.findByConversationOrderByMessageOrderAsc(conversation)
        .stream()
        .map(mapper::toMessageDTO)
        .toList();
  }

  @Transactional
  public JobPostDetailDTO restoreVersion(Long jobId, Long versionId) {
    JobPostEntity job = findJob(jobId);
    JobPostVersionEntity version = versionRepository.findById(versionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "JD 版本不存在"));
    if (!version.getJobPost().getId().equals(job.getId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "JD 版本不属于当前岗位");
    }
    job.setCurrentVersionId(version.getId());
    List<ComplianceRiskItemDTO> risks = riskRepository.findByJobPostVersionOrderBySortOrderAsc(version)
        .stream()
        .map(mapper::toRiskDTO)
        .toList();
    JobProfileDTO profile = profileRepository.findByJobPost(job)
        .map(mapper::toProfileDTO)
        .orElse(null);
    return new JobPostDetailDTO(
        mapper.toJobPostDTO(job),
        mapper.toVersionDTO(version),
        risks,
        profile
    );
  }

  @Transactional(readOnly = true)
  public List<ComplianceRiskItemDTO> listRisks(Long versionId) {
    JobPostVersionEntity version = versionRepository.findById(versionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "JD 版本不存在"));
    return riskRepository.findByJobPostVersionOrderBySortOrderAsc(version)
        .stream()
        .map(mapper::toRiskDTO)
        .toList();
  }

  public List<ComplianceRiskItemDTO> reviewCompliance(Long versionId, String provider) {
    JobPostVersionEntity version = transactionalExecutor.call(() ->
        versionRepository.findById(versionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "JD 版本不存在")));
    ComplianceReviewOutput output = jobAiService.reviewCompliance(version.getContent(), provider);
    return transactionalExecutor.call(() -> saveRisks(versionId, output));
  }

  public JobProfileDTO extractProfile(Long versionId, String provider) {
    JobPostVersionEntity version = transactionalExecutor.call(() ->
        versionRepository.findById(versionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "JD 版本不存在")));
    JobProfileOutput output = jobAiService.extractProfile(version.getContent(), provider);
    return transactionalExecutor.call(() -> saveProfile(versionId, output));
  }

  @Transactional(readOnly = true)
  public JobProfileDTO getProfile(Long jobId) {
    JobPostEntity job = findJob(jobId);
    return profileRepository.findByJobPost(job)
        .map(mapper::toProfileDTO)
        .orElse(null);
  }

  private JobDraftPreparation prepareChat(Long jobId, JobChatRequest request) {
    JobPostEntity job = findJob(jobId);
    JobConversationEntity conversation = request.conversationId() == null
        ? findOrCreateConversation(job)
        : findConversation(request.conversationId(), job);
    JobPostVersionEntity currentVersion = findCurrentVersion(job);
    String history = buildConversationHistory(conversation);
    var context = new JobAiService.JobDraftContext(
        job.getTitle(),
        job.getDepartment(),
        job.getLocation(),
        job.getEmploymentType(),
        job.getSeniority(),
        currentVersion == null ? "" : currentVersion.getContent(),
        history,
        request.message()
    );
    return new JobDraftPreparation(job.getId(), conversation.getId(), context);
  }

  private JobChatResponse saveChatResult(
      JobDraftPreparation preparation,
      String userMessage,
      JobDraftOutput draftOutput,
      ComplianceReviewOutput complianceOutput,
      JobProfileOutput profileOutput
  ) {
    JobPostEntity job = findJob(preparation.jobId());
    JobConversationEntity conversation = findConversation(preparation.conversationId(), job);
    saveMessage(conversation, MessageRole.USER, userMessage, null);

    applyJobMetadata(job, draftOutput);
    JobPostVersionEntity version = createVersion(
        job,
        draftOutput.jdContent(),
        toJson(draftOutput.structuredContent()),
        draftOutput.changeSummary(),
        true
    );
    job.setCurrentVersionId(version.getId());
    List<ComplianceRiskItemDTO> risks = saveRisks(version.getId(), complianceOutput);
    JobProfileDTO profile = saveProfile(version.getId(), profileOutput);
    JobConversationMessageEntity assistantMessage = saveMessage(
        conversation,
        MessageRole.ASSISTANT,
        draftOutput.assistantReply() == null || draftOutput.assistantReply().isBlank()
            ? "已生成新的 JD 草稿，并完成合规初筛与岗位画像抽取。"
            : draftOutput.assistantReply(),
        version.getId()
    );

    return new JobChatResponse(
        mapper.toJobPostDTO(job),
        mapper.toConversationDTO(conversation),
        mapper.toMessageDTO(assistantMessage),
        mapper.toVersionDTO(version),
        risks,
        profile
    );
  }

  private JobPostVersionEntity createVersion(
      JobPostEntity job,
      String content,
      String structuredContentJson,
      String changeSummary,
      boolean createdByAi
  ) {
    int nextVersionNo = versionRepository.findFirstByJobPostOrderByVersionNoDesc(job)
        .map(JobPostVersionEntity::getVersionNo)
        .orElse(0) + 1;
    JobPostVersionEntity version = new JobPostVersionEntity();
    version.setJobPost(job);
    version.setVersionNo(nextVersionNo);
    version.setContent(content);
    version.setStructuredContentJson(structuredContentJson);
    version.setChangeSummary(changeSummary);
    version.setCreatedByAi(createdByAi);
    return versionRepository.save(version);
  }

  private List<ComplianceRiskItemDTO> saveRisks(Long versionId, ComplianceReviewOutput output) {
    JobPostVersionEntity version = versionRepository.findById(versionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "JD 版本不存在"));
    riskRepository.deleteByJobPostVersion(version);
    List<RiskItemOutput> riskOutputs = output == null || output.risks() == null
        ? List.of()
        : output.risks();
    for (int i = 0; i < riskOutputs.size(); i++) {
      RiskItemOutput item = riskOutputs.get(i);
      ComplianceRiskItemEntity entity = new ComplianceRiskItemEntity();
      entity.setJobPostVersion(version);
      entity.setRiskLevel(parseRiskLevel(item.riskLevel()));
      entity.setRiskType(nonBlank(item.riskType(), "通用合规风险"));
      entity.setMatchedText(item.matchedText());
      entity.setDescription(nonBlank(item.description(), "该表达可能存在招聘合规风险"));
      entity.setSuggestion(item.suggestion());
      entity.setBasis(item.basis());
      entity.setSortOrder(i);
      riskRepository.save(entity);
    }
    return riskRepository.findByJobPostVersionOrderBySortOrderAsc(version)
        .stream()
        .map(mapper::toRiskDTO)
        .toList();
  }

  private JobProfileDTO saveProfile(Long versionId, JobProfileOutput output) {
    JobPostVersionEntity version = versionRepository.findById(versionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "JD 版本不存在"));
    JobPostEntity job = version.getJobPost();
    JobProfileEntity profile = profileRepository.findByJobPost(job)
        .orElseGet(JobProfileEntity::new);
    profile.setJobPost(job);
    profile.setJobPostVersion(version);
    profile.setHardRequirementsJson(toJson(output == null ? List.of() : output.hardRequirements()));
    profile.setCompetenciesJson(toJson(output == null ? List.of() : output.competencies()));
    profile.setBonusItemsJson(toJson(output == null ? List.of() : output.bonusItems()));
    profile.setEliminationItemsJson(toJson(output == null ? List.of() : output.eliminationItems()));
    profile.setVerificationPointsJson(toJson(output == null ? List.of() : output.verificationPoints()));
    return mapper.toProfileDTO(profileRepository.save(profile));
  }

  private JobConversationEntity createConversation(JobPostEntity job) {
    JobConversationEntity conversation = new JobConversationEntity();
    conversation.setJobPost(job);
    conversation.setTitle(job.getTitle() + " JD 优化");
    return conversationRepository.save(conversation);
  }

  private JobConversationEntity findOrCreateConversation(JobPostEntity job) {
    return conversationRepository.findFirstByJobPostOrderByUpdatedAtDesc(job)
        .orElseGet(() -> createConversation(job));
  }

  private JobConversationEntity findConversation(Long conversationId, JobPostEntity job) {
    JobConversationEntity conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "JD 对话不存在"));
    if (!conversation.getJobPost().getId().equals(job.getId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "JD 对话不属于当前岗位");
    }
    return conversation;
  }

  private JobConversationMessageEntity saveMessage(
      JobConversationEntity conversation,
      MessageRole role,
      String content,
      Long relatedVersionId
  ) {
    JobConversationMessageEntity message = new JobConversationMessageEntity();
    message.setConversation(conversation);
    message.setRole(role);
    message.setContent(content);
    message.setRelatedVersionId(relatedVersionId);
    message.setMessageOrder((int) messageRepository.countByConversation(conversation) + 1);
    conversation.setUpdatedAt(LocalDateTime.now());
    return messageRepository.save(message);
  }

  private String buildConversationHistory(JobConversationEntity conversation) {
    return messageRepository.findByConversationOrderByMessageOrderAsc(conversation)
        .stream()
        .sorted(Comparator.comparing(JobConversationMessageEntity::getMessageOrder))
        .map(message -> message.getRole().name().toLowerCase() + ": " + message.getContent())
        .collect(Collectors.joining("\n\n"));
  }

  private void applyJobMetadata(JobPostEntity job, JobDraftOutput draftOutput) {
    if (draftOutput.title() != null && !draftOutput.title().isBlank()) {
      job.setTitle(draftOutput.title().trim());
    }
    if (draftOutput.department() != null && !draftOutput.department().isBlank()) {
      job.setDepartment(draftOutput.department().trim());
    }
    if (draftOutput.location() != null && !draftOutput.location().isBlank()) {
      job.setLocation(draftOutput.location().trim());
    }
    if (draftOutput.employmentType() != null && !draftOutput.employmentType().isBlank()) {
      job.setEmploymentType(draftOutput.employmentType().trim());
    }
    if (draftOutput.seniority() != null && !draftOutput.seniority().isBlank()) {
      job.setSeniority(draftOutput.seniority().trim());
    }
  }

  private JobPostEntity findJob(Long jobId) {
    return jobPostRepository.findById(jobId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "岗位不存在"));
  }

  private JobPostVersionEntity findCurrentVersion(JobPostEntity job) {
    if (job.getCurrentVersionId() == null) {
      return null;
    }
    return versionRepository.findById(job.getCurrentVersionId()).orElse(null);
  }

  private RiskLevel parseRiskLevel(String raw) {
    if (raw == null) {
      return RiskLevel.LOW;
    }
    try {
      return RiskLevel.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return RiskLevel.LOW;
    }
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value == null ? List.of() : value);
    } catch (Exception e) {
      log.error("序列化招聘模块 JSON 失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存招聘数据失败");
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String nonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private record JobDraftPreparation(
      Long jobId,
      Long conversationId,
      JobAiService.JobDraftContext context
  ) {}
}
