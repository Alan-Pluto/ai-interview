package interview.guide.modules.recruitment.message;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.transaction.TransactionalExecutor;
import interview.guide.modules.recruitment.job.RecruitmentMapper;
import interview.guide.modules.recruitment.model.CandidateJobMatchEntity;
import interview.guide.modules.recruitment.model.CandidateMessageAiOutput;
import interview.guide.modules.recruitment.model.CandidateMessageConversationDTO;
import interview.guide.modules.recruitment.model.CandidateMessageConversationEntity;
import interview.guide.modules.recruitment.model.CandidateMessageConversationMessageDTO;
import interview.guide.modules.recruitment.model.CandidateMessageConversationMessageEntity;
import interview.guide.modules.recruitment.model.CandidateMessageConversationMessageEntity.MessageRole;
import interview.guide.modules.recruitment.model.CandidateMessageDraftDTO;
import interview.guide.modules.recruitment.model.CandidateMessageDraftEntity;
import interview.guide.modules.recruitment.model.CandidateMessageDraftResponse;
import interview.guide.modules.recruitment.model.CreateCandidateMessageDraftRequest;
import interview.guide.modules.recruitment.model.InterviewGuideEntity;
import interview.guide.modules.recruitment.model.JobPostEntity;
import interview.guide.modules.recruitment.model.JobPostVersionEntity;
import interview.guide.modules.recruitment.model.OptimizeCandidateMessageDraftRequest;
import interview.guide.modules.recruitment.model.UpdateCandidateMessageDraftRequest;
import interview.guide.modules.recruitment.repository.CandidateJobMatchRepository;
import interview.guide.modules.recruitment.repository.CandidateMessageConversationMessageRepository;
import interview.guide.modules.recruitment.repository.CandidateMessageConversationRepository;
import interview.guide.modules.recruitment.repository.CandidateMessageDraftRepository;
import interview.guide.modules.recruitment.repository.InterviewGuideRepository;
import interview.guide.modules.recruitment.repository.JobPostRepository;
import interview.guide.modules.recruitment.repository.JobPostVersionRepository;
import interview.guide.modules.resume.model.ResumeEntity;
import interview.guide.modules.resume.repository.ResumeRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateMessageService {

  private final CandidateMessageDraftRepository draftRepository;
  private final CandidateMessageConversationRepository conversationRepository;
  private final CandidateMessageConversationMessageRepository messageRepository;
  private final JobPostRepository jobPostRepository;
  private final JobPostVersionRepository versionRepository;
  private final ResumeRepository resumeRepository;
  private final CandidateJobMatchRepository matchRepository;
  private final InterviewGuideRepository guideRepository;
  private final CandidateMessageAiService aiService;
  private final RecruitmentMapper mapper;
  private final ObjectMapper objectMapper;
  private final TransactionalExecutor transactionalExecutor;

  public CandidateMessageDraftResponse createDraft(CreateCandidateMessageDraftRequest request) {
    MessagePreparation preparation = transactionalExecutor.call(() -> prepareCreate(request));
    CandidateMessageAiOutput output = aiService.generate(preparation.context(), request.provider());
    return transactionalExecutor.call(() -> saveNewDraft(preparation, output));
  }

  public CandidateMessageDraftResponse optimizeDraft(
      Long draftId,
      OptimizeCandidateMessageDraftRequest request
  ) {
    MessagePreparation preparation = transactionalExecutor.call(() -> prepareOptimize(draftId, request));
    CandidateMessageAiOutput output = aiService.generate(preparation.context(), request.provider());
    return transactionalExecutor.call(() -> saveOptimizedDraft(preparation, request.instruction(), output));
  }

  @Transactional(readOnly = true)
  public List<CandidateMessageDraftDTO> listByJob(Long jobId) {
    JobPostEntity job = findJob(jobId);
    return draftRepository.findByJobPostOrderByUpdatedAtDesc(job)
        .stream()
        .map(mapper::toCandidateMessageDraftDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public CandidateMessageDraftDTO getDraft(Long draftId) {
    return draftRepository.findById(draftId)
        .map(mapper::toCandidateMessageDraftDTO)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "沟通文案草稿不存在"));
  }

  @Transactional(readOnly = true)
  public List<CandidateMessageConversationMessageDTO> listMessages(Long draftId) {
    CandidateMessageDraftEntity draft = findDraft(draftId);
    CandidateMessageConversationEntity conversation = findConversation(draft);
    return messageRepository.findByConversationOrderByMessageOrderAsc(conversation)
        .stream()
        .map(mapper::toCandidateMessageMessageDTO)
        .toList();
  }

  @Transactional
  public CandidateMessageDraftDTO updateDraft(Long draftId, UpdateCandidateMessageDraftRequest request) {
    CandidateMessageDraftEntity draft = findDraft(draftId);
    draft.setSubject(request.subject());
    draft.setContent(request.content());
    return mapper.toCandidateMessageDraftDTO(draft);
  }

  private MessagePreparation prepareCreate(CreateCandidateMessageDraftRequest request) {
    JobPostEntity job = findJob(request.jobPostId());
    JobPostVersionEntity version = findCurrentVersion(job);
    CandidateJobMatchEntity match = findMatch(request.matchId(), job);
    ResumeEntity resume = resolveResume(request.resumeId(), match);
    InterviewGuideEntity guide = findGuide(request.interviewGuideId(), job);
    String matchReport = match == null ? "未选择匹配报告。" : toJson(List.of(
        match.getSummary(),
        match.getStrengthsJson(),
        match.getRisksJson(),
        match.getRequirementChecksJson(),
        match.getVerificationQuestionsJson()
    ));
    String guideText = guide == null ? "未选择面试题纲。" : toJson(List.of(
        guide.getInterviewRound(),
        guide.getMode().name(),
        guide.getOutlineJson(),
        guide.getScorecardJson()
    ));
    var context = new CandidateMessageAiService.MessageContext(
        request.messageType(),
        request.channel(),
        request.stage(),
        request.tone(),
        job.getTitle(),
        version.getContent(),
        resume == null ? "未选择候选人简历。" : resume.getResumeText(),
        matchReport,
        guideText,
        "新建草稿，暂无现有文案。",
        "新建草稿，暂无历史对话。",
        request.extraInstruction()
    );
    return new MessagePreparation(
        job.getId(),
        resume == null ? null : resume.getId(),
        match == null ? null : match.getId(),
        guide == null ? null : guide.getId(),
        request.messageType(),
        request.channel(),
        request.stage(),
        request.tone(),
        context,
        null
    );
  }

  private MessagePreparation prepareOptimize(Long draftId, OptimizeCandidateMessageDraftRequest request) {
    CandidateMessageDraftEntity draft = findDraft(draftId);
    CandidateMessageConversationEntity conversation = findConversation(draft);
    JobPostEntity job = draft.getJobPost();
    JobPostVersionEntity version = findCurrentVersion(job);
    ResumeEntity resume = draft.getResume();
    CandidateJobMatchEntity match = draft.getMatch();
    InterviewGuideEntity guide = draft.getInterviewGuide();
    String history = toJson(messageRepository.findByConversationOrderByMessageOrderAsc(conversation)
        .stream()
        .map(message -> message.getRole().name() + ": " + message.getContent())
        .toList());
    String matchReport = match == null ? "未选择匹配报告。" : toJson(List.of(
        match.getSummary(),
        match.getStrengthsJson(),
        match.getRisksJson(),
        match.getRequirementChecksJson(),
        match.getVerificationQuestionsJson()
    ));
    String guideText = guide == null ? "未选择面试题纲。" : toJson(List.of(
        guide.getInterviewRound(),
        guide.getMode().name(),
        guide.getOutlineJson(),
        guide.getScorecardJson()
    ));
    var context = new CandidateMessageAiService.MessageContext(
        draft.getMessageType(),
        draft.getChannel(),
        draft.getStage(),
        draft.getTone(),
        job.getTitle(),
        version.getContent(),
        resume == null ? "未选择候选人简历。" : resume.getResumeText(),
        matchReport,
        guideText,
        draft.getContent(),
        history,
        request.instruction()
    );
    return new MessagePreparation(
        job.getId(),
        resume == null ? null : resume.getId(),
        match == null ? null : match.getId(),
        guide == null ? null : guide.getId(),
        draft.getMessageType(),
        draft.getChannel(),
        draft.getStage(),
        draft.getTone(),
        context,
        draft.getId()
    );
  }

  private CandidateMessageDraftResponse saveNewDraft(
      MessagePreparation preparation,
      CandidateMessageAiOutput output
  ) {
    validateOutput(output);
    JobPostEntity job = findJob(preparation.jobId());
    CandidateMessageDraftEntity draft = new CandidateMessageDraftEntity();
    draft.setJobPost(job);
    fillRelations(draft, preparation);
    draft.setMessageType(preparation.messageType());
    draft.setChannel(preparation.channel());
    draft.setStage(preparation.stage());
    draft.setTone(preparation.tone());
    applyOutput(draft, output);
    CandidateMessageDraftEntity savedDraft = draftRepository.save(draft);

    CandidateMessageConversationEntity conversation = new CandidateMessageConversationEntity();
    conversation.setDraft(savedDraft);
    conversation.setTitle(buildConversationTitle(savedDraft));
    CandidateMessageConversationEntity savedConversation = conversationRepository.save(conversation);
    CandidateMessageConversationMessageEntity assistantMessage = saveConversationMessage(
        savedConversation, MessageRole.ASSISTANT, output.content());
    return toResponse(savedDraft, savedConversation, assistantMessage);
  }

  private CandidateMessageDraftResponse saveOptimizedDraft(
      MessagePreparation preparation,
      String instruction,
      CandidateMessageAiOutput output
  ) {
    validateOutput(output);
    CandidateMessageDraftEntity draft = findDraft(preparation.draftId());
    CandidateMessageConversationEntity conversation = findConversation(draft);
    saveConversationMessage(conversation, MessageRole.USER, instruction);
    applyOutput(draft, output);
    CandidateMessageDraftEntity savedDraft = draftRepository.save(draft);
    CandidateMessageConversationMessageEntity assistantMessage = saveConversationMessage(
        conversation, MessageRole.ASSISTANT, output.content());
    return toResponse(savedDraft, conversation, assistantMessage);
  }

  private void fillRelations(CandidateMessageDraftEntity draft, MessagePreparation preparation) {
    if (preparation.resumeId() != null) {
      draft.setResume(resumeRepository.findById(preparation.resumeId())
          .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND)));
    }
    if (preparation.matchId() != null) {
      draft.setMatch(matchRepository.findById(preparation.matchId())
          .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "匹配报告不存在")));
    }
    if (preparation.interviewGuideId() != null) {
      draft.setInterviewGuide(guideRepository.findById(preparation.interviewGuideId())
          .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "面试题纲不存在")));
    }
  }

  private void applyOutput(CandidateMessageDraftEntity draft, CandidateMessageAiOutput output) {
    draft.setSubject(blankToNull(output.subject()));
    draft.setContent(output.content());
    draft.setKeyPointsJson(toJson(output.keyPoints()));
    draft.setCautionNotesJson(toJson(output.cautionNotes()));
  }

  private CandidateMessageDraftResponse toResponse(
      CandidateMessageDraftEntity draft,
      CandidateMessageConversationEntity conversation,
      CandidateMessageConversationMessageEntity assistantMessage
  ) {
    return new CandidateMessageDraftResponse(
        mapper.toCandidateMessageDraftDTO(draft),
        mapper.toCandidateMessageConversationDTO(conversation),
        mapper.toCandidateMessageMessageDTO(assistantMessage)
    );
  }

  private CandidateMessageConversationMessageEntity saveConversationMessage(
      CandidateMessageConversationEntity conversation,
      MessageRole role,
      String content
  ) {
    CandidateMessageConversationMessageEntity message = new CandidateMessageConversationMessageEntity();
    message.setConversation(conversation);
    message.setRole(role);
    message.setContent(content);
    message.setMessageOrder((int) messageRepository.countByConversation(conversation) + 1);
    return messageRepository.save(message);
  }

  private CandidateJobMatchEntity findMatch(Long matchId, JobPostEntity job) {
    if (matchId == null) {
      return null;
    }
    CandidateJobMatchEntity match = matchRepository.findById(matchId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "匹配报告不存在"));
    if (!Objects.equals(match.getJobPost().getId(), job.getId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "匹配报告不属于当前岗位");
    }
    return match;
  }

  private ResumeEntity resolveResume(Long resumeId, CandidateJobMatchEntity match) {
    if (match != null) {
      return match.getResume();
    }
    if (resumeId == null) {
      return null;
    }
    ResumeEntity resume = resumeRepository.findById(resumeId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
    if (resume.getResumeText() == null || resume.getResumeText().isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该简历没有可用于生成文案的解析文本");
    }
    return resume;
  }

  private InterviewGuideEntity findGuide(Long guideId, JobPostEntity job) {
    if (guideId == null) {
      return null;
    }
    InterviewGuideEntity guide = guideRepository.findById(guideId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "面试题纲不存在"));
    if (!Objects.equals(guide.getJobPost().getId(), job.getId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题纲不属于当前岗位");
    }
    return guide;
  }

  private CandidateMessageDraftEntity findDraft(Long draftId) {
    return draftRepository.findById(draftId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "沟通文案草稿不存在"));
  }

  private CandidateMessageConversationEntity findConversation(CandidateMessageDraftEntity draft) {
    return conversationRepository.findByDraft(draft)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "沟通文案对话不存在"));
  }

  private JobPostEntity findJob(Long jobId) {
    return jobPostRepository.findById(jobId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "岗位不存在"));
  }

  private JobPostVersionEntity findCurrentVersion(JobPostEntity job) {
    if (job.getCurrentVersionId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先在 JD 助手中生成 JD 草稿");
    }
    return versionRepository.findById(job.getCurrentVersionId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "当前 JD 版本不存在"));
  }

  private void validateOutput(CandidateMessageAiOutput output) {
    if (output == null || output.content() == null || output.content().isBlank()) {
      throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "候选人沟通文案生成结果为空，请重试");
    }
  }

  private String buildConversationTitle(CandidateMessageDraftEntity draft) {
    return draft.getMessageType() + " · " + draft.getChannel();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value == null ? List.of() : value);
    } catch (Exception e) {
      log.error("序列化候选人沟通文案失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存候选人沟通文案失败");
    }
  }

  private record MessagePreparation(
      Long jobId,
      Long resumeId,
      Long matchId,
      Long interviewGuideId,
      String messageType,
      String channel,
      String stage,
      String tone,
      CandidateMessageAiService.MessageContext context,
      Long draftId
  ) {}
}
