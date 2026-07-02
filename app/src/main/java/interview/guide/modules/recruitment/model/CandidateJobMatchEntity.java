package interview.guide.modules.recruitment.model;

import interview.guide.modules.resume.model.ResumeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recruitment_candidate_job_matches")
@Getter
@Setter
@NoArgsConstructor
public class CandidateJobMatchEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_post_id", nullable = false)
  private JobPostEntity jobPost;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_post_version_id", nullable = false)
  private JobPostVersionEntity jobPostVersion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resume_id", nullable = false)
  private ResumeEntity resume;

  @Column(length = 80)
  private String batchId;

  @Column(nullable = false)
  private Integer matchScore;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private MatchRecommendation recommendation;

  @Column(columnDefinition = "TEXT")
  private String summary;

  @Column(columnDefinition = "TEXT")
  private String strengthsJson;

  @Column(columnDefinition = "TEXT")
  private String risksJson;

  @Column(columnDefinition = "TEXT")
  private String requirementChecksJson;

  @Column(columnDefinition = "TEXT")
  private String verificationQuestionsJson;

  @Column(nullable = false, length = 20)
  private String status = "COMPLETED";

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  public enum MatchRecommendation {
    RECOMMENDED,
    CONSIDER,
    NOT_RECOMMENDED
  }
}
