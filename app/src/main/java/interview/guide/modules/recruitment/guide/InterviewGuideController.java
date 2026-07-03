package interview.guide.modules.recruitment.guide;

import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.modules.recruitment.model.CreateInterviewGuideRequest;
import interview.guide.modules.recruitment.model.InterviewGuideDTO;
import interview.guide.modules.recruitment.model.UpdateInterviewGuideRequest;
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
public class InterviewGuideController {

  private final InterviewGuideService guideService;

  @PostMapping("/interview-guides")
  @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 20)
  @RateLimit(dimension = RateLimit.Dimension.IP, count = 10)
  public Result<InterviewGuideDTO> generate(@Valid @RequestBody CreateInterviewGuideRequest request) {
    return Result.success(guideService.generate(request));
  }

  @GetMapping("/interview-guides/{guideId}")
  public Result<InterviewGuideDTO> getGuide(@PathVariable Long guideId) {
    return Result.success(guideService.getGuide(guideId));
  }

  @GetMapping("/interview-guides")
  public Result<List<InterviewGuideDTO>> listRecentGuides() {
    return Result.success(guideService.listRecentGuides());
  }

  @PutMapping("/interview-guides/{guideId}")
  public Result<InterviewGuideDTO> updateGuide(
      @PathVariable Long guideId,
      @Valid @RequestBody UpdateInterviewGuideRequest request
  ) {
    return Result.success(guideService.updateGuide(guideId, request));
  }

  @DeleteMapping("/interview-guides/{guideId}")
  public Result<Void> deleteGuide(@PathVariable Long guideId) {
    guideService.deleteGuide(guideId);
    return Result.success();
  }

  @GetMapping("/jobs/{jobId}/interview-guides")
  public Result<List<InterviewGuideDTO>> listByJob(@PathVariable Long jobId) {
    return Result.success(guideService.listByJob(jobId));
  }
}
