package interview.guide.modules.recruitment.guide;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.transaction.TransactionalExecutor;
import interview.guide.modules.recruitment.job.RecruitmentMapper;
import interview.guide.modules.recruitment.model.CandidateJobMatchEntity;
import interview.guide.modules.recruitment.model.CandidateMessageDraftEntity;
import interview.guide.modules.recruitment.model.CreateInterviewGuideRequest;
import interview.guide.modules.recruitment.model.InterviewGuideAiOutput;
import interview.guide.modules.recruitment.model.InterviewGuideDTO;
import interview.guide.modules.recruitment.model.InterviewGuideEntity;
import interview.guide.modules.recruitment.model.InterviewGuideEntity.GuideMode;
import interview.guide.modules.recruitment.model.JobPostEntity;
import interview.guide.modules.recruitment.model.JobPostVersionEntity;
import interview.guide.modules.recruitment.model.JobProfileEntity;
import interview.guide.modules.recruitment.model.UpdateInterviewGuideRequest;
import interview.guide.modules.recruitment.repository.CandidateJobMatchRepository;
import interview.guide.modules.recruitment.repository.CandidateMessageDraftRepository;
import interview.guide.modules.recruitment.repository.InterviewGuideRepository;
import interview.guide.modules.recruitment.repository.JobPostRepository;
import interview.guide.modules.recruitment.repository.JobPostVersionRepository;
import interview.guide.modules.recruitment.repository.JobProfileRepository;
import interview.guide.modules.resume.model.ResumeEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewGuideService {

  private static final int DEFAULT_DURATION_MINUTES = 45;

  private final InterviewGuideRepository guideRepository;
  private final JobPostRepository jobPostRepository;
  private final JobPostVersionRepository versionRepository;
  private final JobProfileRepository profileRepository;
  private final CandidateJobMatchRepository matchRepository;
  private final CandidateMessageDraftRepository draftRepository;
  private final InterviewGuideAiService aiService;
  private final RecruitmentMapper mapper;
  private final ObjectMapper objectMapper;
  private final TransactionalExecutor transactionalExecutor;

  public InterviewGuideDTO generate(CreateInterviewGuideRequest request) {
    GuidePreparation preparation = transactionalExecutor.call(() -> prepare(request));
    InterviewGuideAiOutput output = aiService.generate(preparation.context(), request.provider());
    return transactionalExecutor.call(() -> saveGuide(preparation, output));
  }

  @Transactional(readOnly = true)
  public InterviewGuideDTO getGuide(Long guideId) {
    return guideRepository.findById(guideId)
        .map(mapper::toInterviewGuideDTO)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "面试题纲不存在"));
  }

  @Transactional(readOnly = true)
  public List<InterviewGuideDTO> listRecentGuides() {
    return guideRepository.findAllByOrderByUpdatedAtDesc()
        .stream()
        .map(mapper::toInterviewGuideDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<InterviewGuideDTO> listByJob(Long jobId) {
    JobPostEntity job = findJob(jobId);
    return guideRepository.findByJobPostOrderByUpdatedAtDesc(job)
        .stream()
        .map(mapper::toInterviewGuideDTO)
        .toList();
  }

  @Transactional
  public InterviewGuideDTO updateGuide(Long guideId, UpdateInterviewGuideRequest request) {
    InterviewGuideEntity guide = guideRepository.findById(guideId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "面试题纲不存在"));
    guide.setOutlineJson(request.outlineJson());
    guide.setScorecardJson(request.scorecardJson());
    return mapper.toInterviewGuideDTO(guide);
  }

  @Transactional
  public void deleteGuide(Long guideId) {
    InterviewGuideEntity guide = guideRepository.findById(guideId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "面试题纲不存在"));
    for (CandidateMessageDraftEntity draft : draftRepository.findByInterviewGuide(guide)) {
      draft.setInterviewGuide(null);
    }
    guideRepository.delete(guide);
  }

  private GuidePreparation prepare(CreateInterviewGuideRequest request) {
    JobPostEntity job = findJob(request.jobPostId());
    JobPostVersionEntity version = findCurrentVersion(job);
    GuideMode mode = parseMode(request.mode());
    CandidateJobMatchEntity match = null;
    ResumeEntity resume = null;
    String resumeText = "无候选人简历，生成通用题纲。";
    String matchReport = "无匹配报告。";

    if (mode == GuideMode.CANDIDATE_SPECIFIC) {
      if (request.matchId() == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "定制题纲需要选择匹配报告");
      }
      match = matchRepository.findById(request.matchId())
          .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "匹配报告不存在"));
      if (!match.getJobPost().getId().equals(job.getId())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "匹配报告不属于当前岗位");
      }
      resume = match.getResume();
      resumeText = resume.getResumeText();
      matchReport = toJson(List.of(
          match.getSummary(),
          match.getStrengthsJson(),
          match.getRisksJson(),
          match.getRequirementChecksJson(),
          match.getVerificationQuestionsJson()
      ));
    }

    JobProfileEntity profile = profileRepository.findByJobPost(job).orElse(null);
    String profileText = profile == null ? "暂无岗位画像" : toJson(List.of(
        profile.getHardRequirementsJson(),
        profile.getCompetenciesJson(),
        profile.getBonusItemsJson(),
        profile.getEliminationItemsJson(),
        profile.getVerificationPointsJson()
    ));
    int duration = request.durationMinutes() == null || request.durationMinutes() <= 0
        ? DEFAULT_DURATION_MINUTES
        : request.durationMinutes();
    var context = new InterviewGuideAiService.GuideContext(
        mode.name(),
        request.interviewRound(),
        duration,
        job.getTitle(),
        version.getContent(),
        profileText,
        resumeText,
        matchReport
    );
    return new GuidePreparation(job.getId(), resume == null ? null : resume.getId(),
        match == null ? null : match.getId(), mode, request.interviewRound(), duration, context);
  }

  private InterviewGuideDTO saveGuide(GuidePreparation preparation, InterviewGuideAiOutput output) {
    if (output == null) {
      throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "面试题纲生成结果为空，请重试");
    }
    JobPostEntity job = findJob(preparation.jobId());
    InterviewGuideEntity guide = new InterviewGuideEntity();
    guide.setJobPost(job);
    if (preparation.matchId() != null) {
      CandidateJobMatchEntity match = matchRepository.findById(preparation.matchId())
          .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "匹配报告不存在"));
      guide.setMatch(match);
      guide.setResume(match.getResume());
    }
    guide.setInterviewRound(preparation.interviewRound());
    guide.setDurationMinutes(preparation.durationMinutes());
    guide.setMode(preparation.mode());
    guide.setOutlineJson(toJson(output.outline()));
    guide.setScorecardJson(toJson(output.scorecard()));
    return mapper.toInterviewGuideDTO(guideRepository.save(guide));
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

  private GuideMode parseMode(String value) {
    try {
      return GuideMode.valueOf(value.trim().toUpperCase());
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的题纲生成模式");
    }
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value == null ? List.of() : value);
    } catch (Exception e) {
      log.error("序列化面试题纲失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存面试题纲失败");
    }
  }

  private record GuidePreparation(
      Long jobId,
      Long resumeId,
      Long matchId,
      GuideMode mode,
      String interviewRound,
      Integer durationMinutes,
      InterviewGuideAiService.GuideContext context
  ) {}
}
