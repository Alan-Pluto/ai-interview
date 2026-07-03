package interview.guide.modules.recruitment.match;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.transaction.TransactionalExecutor;
import interview.guide.modules.recruitment.job.RecruitmentMapper;
import interview.guide.modules.recruitment.model.CandidateJobMatchDTO;
import interview.guide.modules.recruitment.model.CandidateJobMatchEntity;
import interview.guide.modules.recruitment.model.CandidateJobMatchEntity.MatchRecommendation;
import interview.guide.modules.recruitment.model.CandidateMessageDraftEntity;
import interview.guide.modules.recruitment.model.CandidateMatchAiOutput;
import interview.guide.modules.recruitment.model.CreateCandidateMatchRequest;
import interview.guide.modules.recruitment.model.InterviewGuideEntity;
import interview.guide.modules.recruitment.model.JobPostEntity;
import interview.guide.modules.recruitment.model.JobPostVersionEntity;
import interview.guide.modules.recruitment.model.JobProfileEntity;
import interview.guide.modules.recruitment.repository.CandidateJobMatchRepository;
import interview.guide.modules.recruitment.repository.CandidateMessageDraftRepository;
import interview.guide.modules.recruitment.repository.InterviewGuideRepository;
import interview.guide.modules.recruitment.repository.JobPostRepository;
import interview.guide.modules.recruitment.repository.JobPostVersionRepository;
import interview.guide.modules.recruitment.repository.JobProfileRepository;
import interview.guide.modules.resume.model.ResumeEntity;
import interview.guide.modules.resume.repository.ResumeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateJobMatchService {

  private final CandidateJobMatchRepository matchRepository;
  private final JobPostRepository jobPostRepository;
  private final JobPostVersionRepository versionRepository;
  private final JobProfileRepository profileRepository;
  private final ResumeRepository resumeRepository;
  private final InterviewGuideRepository guideRepository;
  private final CandidateMessageDraftRepository draftRepository;
  private final CandidateJobMatchAiService aiService;
  private final RecruitmentMapper mapper;
  private final ObjectMapper objectMapper;
  private final TransactionalExecutor transactionalExecutor;

  public CandidateJobMatchDTO matchSingleCandidate(Long jobId, CreateCandidateMatchRequest request) {
    MatchPreparation preparation = transactionalExecutor.call(() -> prepare(jobId, request));
    CandidateMatchAiOutput output = aiService.match(preparation.context(), request.provider());
    return transactionalExecutor.call(() -> saveMatch(preparation, output));
  }

  @Transactional(readOnly = true)
  public List<CandidateJobMatchDTO> listRecentMatches() {
    return matchRepository.findAllByOrderByCreatedAtDesc()
        .stream()
        .map(mapper::toMatchDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CandidateJobMatchDTO> listMatches(Long jobId) {
    JobPostEntity job = findJob(jobId);
    return matchRepository.findByJobPostOrderByCreatedAtDesc(job)
        .stream()
        .map(mapper::toMatchDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public CandidateJobMatchDTO getMatch(Long matchId) {
    return matchRepository.findById(matchId)
        .map(mapper::toMatchDTO)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "匹配报告不存在"));
  }

  @Transactional
  public void deleteMatch(Long matchId) {
    CandidateJobMatchEntity match = matchRepository.findById(matchId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "匹配报告不存在"));
    for (InterviewGuideEntity guide : guideRepository.findByMatch(match)) {
      guide.setMatch(null);
    }
    for (CandidateMessageDraftEntity draft : draftRepository.findByMatch(match)) {
      draft.setMatch(null);
    }
    matchRepository.delete(match);
  }

  private MatchPreparation prepare(Long jobId, CreateCandidateMatchRequest request) {
    JobPostEntity job = findJob(jobId);
    JobPostVersionEntity version = findCurrentVersion(job);
    ResumeEntity resume = resumeRepository.findById(request.resumeId())
        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
    if (resume.getResumeText() == null || resume.getResumeText().isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该简历没有可用于匹配的解析文本");
    }
    JobProfileEntity profile = profileRepository.findByJobPost(job).orElse(null);
    String profileText = profile == null ? "暂无岗位画像" : toJson(List.of(
        profile.getHardRequirementsJson(),
        profile.getCompetenciesJson(),
        profile.getBonusItemsJson(),
        profile.getEliminationItemsJson(),
        profile.getVerificationPointsJson()
    ));
    var context = new CandidateJobMatchAiService.MatchContext(
        job.getTitle(),
        version.getContent(),
        profileText,
        resume.getResumeText()
    );
    return new MatchPreparation(job.getId(), version.getId(), resume.getId(), context);
  }

  private CandidateJobMatchDTO saveMatch(MatchPreparation preparation, CandidateMatchAiOutput output) {
    if (output == null) {
      throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "简历匹配结果为空，请重试");
    }
    JobPostEntity job = findJob(preparation.jobId());
    JobPostVersionEntity version = versionRepository.findById(preparation.versionId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "JD 版本不存在"));
    ResumeEntity resume = resumeRepository.findById(preparation.resumeId())
        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

    CandidateJobMatchEntity entity = new CandidateJobMatchEntity();
    entity.setJobPost(job);
    entity.setJobPostVersion(version);
    entity.setResume(resume);
    entity.setBatchId(null);
    entity.setMatchScore(normalizeScore(output.matchScore()));
    entity.setRecommendation(parseRecommendation(output.recommendation()));
    entity.setSummary(output.summary());
    entity.setStrengthsJson(toJson(output.strengths()));
    entity.setRisksJson(toJson(output.risks()));
    entity.setRequirementChecksJson(toJson(output.requirementChecks()));
    entity.setVerificationQuestionsJson(toJson(output.verificationQuestions()));
    return mapper.toMatchDTO(matchRepository.save(entity));
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

  private MatchRecommendation parseRecommendation(String value) {
    if (value == null) {
      return MatchRecommendation.CONSIDER;
    }
    try {
      return MatchRecommendation.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return MatchRecommendation.CONSIDER;
    }
  }

  private int normalizeScore(Integer score) {
    if (score == null) {
      return 0;
    }
    return Math.max(0, Math.min(100, score));
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value == null ? List.of() : value);
    } catch (Exception e) {
      log.error("序列化匹配报告失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存匹配报告失败");
    }
  }

  private record MatchPreparation(
      Long jobId,
      Long versionId,
      Long resumeId,
      CandidateJobMatchAiService.MatchContext context
  ) {}
}
