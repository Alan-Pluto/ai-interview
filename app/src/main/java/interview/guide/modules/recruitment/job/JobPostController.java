package interview.guide.modules.recruitment.job;

import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.modules.recruitment.model.ComplianceRiskItemDTO;
import interview.guide.modules.recruitment.model.CreateJobPostRequest;
import interview.guide.modules.recruitment.model.JobChatRequest;
import interview.guide.modules.recruitment.model.JobChatResponse;
import interview.guide.modules.recruitment.model.JobConversationDTO;
import interview.guide.modules.recruitment.model.JobConversationMessageDTO;
import interview.guide.modules.recruitment.model.JobPostDTO;
import interview.guide.modules.recruitment.model.JobPostDetailDTO;
import interview.guide.modules.recruitment.model.JobPostVersionDTO;
import interview.guide.modules.recruitment.model.JobProfileDTO;
import interview.guide.modules.recruitment.model.SaveJobVersionRequest;
import interview.guide.modules.recruitment.model.UpdateJobPostRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruitment")
public class JobPostController {

  private final JobPostService jobPostService;
  private final ObjectMapper objectMapper;

  @PostMapping("/jobs")
  public Result<JobPostDTO> createJob(@Valid @RequestBody CreateJobPostRequest request) {
    return Result.success(jobPostService.createJob(request));
  }

  @GetMapping("/jobs")
  public Result<List<JobPostDTO>> listJobs() {
    return Result.success(jobPostService.listJobs());
  }

  @GetMapping("/jobs/{jobId}")
  public Result<JobPostDetailDTO> getJob(@PathVariable Long jobId) {
    return Result.success(jobPostService.getJobDetail(jobId));
  }

  @PutMapping("/jobs/{jobId}")
  public Result<JobPostDTO> updateJob(
      @PathVariable Long jobId,
      @Valid @RequestBody UpdateJobPostRequest request
  ) {
    return Result.success(jobPostService.updateJob(jobId, request));
  }

  @PostMapping("/jobs/{jobId}/archive")
  public Result<Void> archiveJob(@PathVariable Long jobId) {
    jobPostService.archiveJob(jobId);
    return Result.success();
  }

  @PostMapping("/jobs/{jobId}/chat")
  @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 20)
  @RateLimit(dimension = RateLimit.Dimension.IP, count = 10)
  public Result<JobChatResponse> chat(
      @PathVariable Long jobId,
      @Valid @RequestBody JobChatRequest request
  ) {
    return Result.success(jobPostService.chat(jobId, request));
  }

  @PostMapping(value = "/jobs/{jobId}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 20)
  @RateLimit(dimension = RateLimit.Dimension.IP, count = 10)
  public Flux<ServerSentEvent<String>> chatStream(
      @PathVariable Long jobId,
      @Valid @RequestBody JobChatRequest request
  ) {
    return Flux.concat(
        event("status", Map.of("message", "正在理解岗位需求...")),
        event("status", Map.of("message", "正在生成 JD 草稿并结合上下文优化...")),
        Flux.defer(() -> Flux.just(jobPostService.chat(jobId, request)))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(response -> Flux.concat(
                event("status", Map.of("message", "正在完成合规审查和岗位画像抽取...")),
                assistantDeltaEvents(response.assistantMessage().content()),
                event("complete", response)
            ))
    ).onErrorResume(e -> {
      log.error("JD 助手流式对话失败: jobId={}", jobId, e);
      return event("error", Map.of("message", e.getMessage() == null ? "JD 生成失败" : e.getMessage()));
    });
  }

  @PostMapping("/jobs/{jobId}/versions")
  public Result<JobPostVersionDTO> saveVersion(
      @PathVariable Long jobId,
      @Valid @RequestBody SaveJobVersionRequest request
  ) {
    return Result.success(jobPostService.saveManualVersion(jobId, request));
  }

  @GetMapping("/jobs/{jobId}/versions")
  public Result<List<JobPostVersionDTO>> listVersions(@PathVariable Long jobId) {
    return Result.success(jobPostService.listVersions(jobId));
  }

  @PostMapping("/jobs/{jobId}/versions/{versionId}/restore")
  public Result<JobPostDetailDTO> restoreVersion(
      @PathVariable Long jobId,
      @PathVariable Long versionId
  ) {
    return Result.success(jobPostService.restoreVersion(jobId, versionId));
  }

  @GetMapping("/jobs/{jobId}/conversations")
  public Result<List<JobConversationDTO>> listConversations(@PathVariable Long jobId) {
    return Result.success(jobPostService.listConversations(jobId));
  }

  @GetMapping("/job-conversations/{conversationId}/messages")
  public Result<List<JobConversationMessageDTO>> listMessages(@PathVariable Long conversationId) {
    return Result.success(jobPostService.listMessages(conversationId));
  }

  @PostMapping("/job-versions/{versionId}/compliance-review")
  @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 20)
  @RateLimit(dimension = RateLimit.Dimension.IP, count = 10)
  public Result<List<ComplianceRiskItemDTO>> reviewCompliance(
      @PathVariable Long versionId,
      @RequestParam(required = false) String provider
  ) {
    return Result.success(jobPostService.reviewCompliance(versionId, provider));
  }

  @GetMapping("/job-versions/{versionId}/compliance-risks")
  public Result<List<ComplianceRiskItemDTO>> listRisks(@PathVariable Long versionId) {
    return Result.success(jobPostService.listRisks(versionId));
  }

  @PostMapping("/job-versions/{versionId}/profile")
  @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 20)
  @RateLimit(dimension = RateLimit.Dimension.IP, count = 10)
  public Result<JobProfileDTO> extractProfile(
      @PathVariable Long versionId,
      @RequestParam(required = false) String provider
  ) {
    return Result.success(jobPostService.extractProfile(versionId, provider));
  }

  @GetMapping("/jobs/{jobId}/profile")
  public Result<JobProfileDTO> getProfile(@PathVariable Long jobId) {
    return Result.success(jobPostService.getProfile(jobId));
  }

  private Flux<ServerSentEvent<String>> event(String eventName, Object data) {
    try {
      return Flux.just(ServerSentEvent.<String>builder()
          .event(eventName)
          .data(objectMapper.writeValueAsString(data))
          .build());
    } catch (Exception e) {
      return Flux.just(ServerSentEvent.<String>builder()
          .event("error")
          .data("{\"message\":\"事件序列化失败\"}")
          .build());
    }
  }

  private Flux<ServerSentEvent<String>> assistantDeltaEvents(String content) {
    if (content == null || content.isBlank()) {
      return Flux.empty();
    }
    return Flux.fromIterable(splitContent(content))
        .map(chunk -> ServerSentEvent.<String>builder()
            .event("assistant_delta")
            .data(toJson(Map.of("content", chunk)))
            .build());
  }

  private List<String> splitContent(String content) {
    int chunkSize = 12;
    List<String> chunks = new java.util.ArrayList<>();
    for (int i = 0; i < content.length(); i += chunkSize) {
      chunks.add(content.substring(i, Math.min(content.length(), i + chunkSize)));
    }
    return chunks;
  }

  private String toJson(Object data) {
    try {
      return objectMapper.writeValueAsString(data);
    } catch (Exception e) {
      return "{\"message\":\"事件序列化失败\"}";
    }
  }
}
