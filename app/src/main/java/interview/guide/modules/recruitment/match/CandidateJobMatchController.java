package interview.guide.modules.recruitment.match;

import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.modules.recruitment.model.CandidateJobMatchDTO;
import interview.guide.modules.recruitment.model.CreateCandidateMatchRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruitment")
public class CandidateJobMatchController {

  private final CandidateJobMatchService matchService;

  @PostMapping("/jobs/{jobId}/matches")
  @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 20)
  @RateLimit(dimension = RateLimit.Dimension.IP, count = 10)
  public Result<CandidateJobMatchDTO> matchSingleCandidate(
      @PathVariable Long jobId,
      @Valid @RequestBody CreateCandidateMatchRequest request
  ) {
    return Result.success(matchService.matchSingleCandidate(jobId, request));
  }

  @GetMapping("/jobs/{jobId}/matches")
  public Result<List<CandidateJobMatchDTO>> listMatches(@PathVariable Long jobId) {
    return Result.success(matchService.listMatches(jobId));
  }

  @GetMapping("/matches/{matchId}")
  public Result<CandidateJobMatchDTO> getMatch(@PathVariable Long matchId) {
    return Result.success(matchService.getMatch(matchId));
  }
}
