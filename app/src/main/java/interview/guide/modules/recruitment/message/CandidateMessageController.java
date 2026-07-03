package interview.guide.modules.recruitment.message;

import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.modules.recruitment.model.CandidateMessageConversationMessageDTO;
import interview.guide.modules.recruitment.model.CandidateMessageDraftDTO;
import interview.guide.modules.recruitment.model.CandidateMessageDraftResponse;
import interview.guide.modules.recruitment.model.CreateCandidateMessageDraftRequest;
import interview.guide.modules.recruitment.model.OptimizeCandidateMessageDraftRequest;
import interview.guide.modules.recruitment.model.UpdateCandidateMessageDraftRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruitment")
public class CandidateMessageController {

  private final CandidateMessageService messageService;

  @PostMapping("/candidate-messages")
  @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 20)
  @RateLimit(dimension = RateLimit.Dimension.IP, count = 10)
  public Result<CandidateMessageDraftResponse> createDraft(
      @Valid @RequestBody CreateCandidateMessageDraftRequest request
  ) {
    return Result.success(messageService.createDraft(request));
  }

  @PostMapping("/candidate-messages/{draftId}/optimize")
  @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 20)
  @RateLimit(dimension = RateLimit.Dimension.IP, count = 10)
  public Result<CandidateMessageDraftResponse> optimizeDraft(
      @PathVariable Long draftId,
      @Valid @RequestBody OptimizeCandidateMessageDraftRequest request
  ) {
    return Result.success(messageService.optimizeDraft(draftId, request));
  }

  @GetMapping("/candidate-messages")
  public Result<List<CandidateMessageDraftDTO>> listRecentDrafts() {
    return Result.success(messageService.listRecentDrafts());
  }

  @GetMapping("/jobs/{jobId}/candidate-messages")
  public Result<List<CandidateMessageDraftDTO>> listByJob(@PathVariable Long jobId) {
    return Result.success(messageService.listByJob(jobId));
  }

  @GetMapping("/candidate-messages/{draftId}")
  public Result<CandidateMessageDraftDTO> getDraft(@PathVariable Long draftId) {
    return Result.success(messageService.getDraft(draftId));
  }

  @GetMapping("/candidate-messages/{draftId}/messages")
  public Result<List<CandidateMessageConversationMessageDTO>> listMessages(@PathVariable Long draftId) {
    return Result.success(messageService.listMessages(draftId));
  }

  @PutMapping("/candidate-messages/{draftId}")
  public Result<CandidateMessageDraftDTO> updateDraft(
      @PathVariable Long draftId,
      @Valid @RequestBody UpdateCandidateMessageDraftRequest request
  ) {
    return Result.success(messageService.updateDraft(draftId, request));
  }

  @DeleteMapping("/candidate-messages/{draftId}")
  public Result<Void> deleteDraft(@PathVariable Long draftId) {
    messageService.deleteDraft(draftId);
    return Result.success();
  }
}
