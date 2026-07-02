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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recruitment_interview_guides")
@Getter
@Setter
@NoArgsConstructor
public class InterviewGuideEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_post_id", nullable = false)
  private JobPostEntity jobPost;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resume_id")
  private ResumeEntity resume;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "match_id")
  private CandidateJobMatchEntity match;

  @Column(nullable = false, length = 40)
  private String interviewRound;

  @Column(nullable = false)
  private Integer durationMinutes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private GuideMode mode;

  @Column(columnDefinition = "TEXT")
  private String outlineJson;

  @Column(columnDefinition = "TEXT")
  private String scorecardJson;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum GuideMode {
    JOB_ONLY,
    CANDIDATE_SPECIFIC
  }
}
