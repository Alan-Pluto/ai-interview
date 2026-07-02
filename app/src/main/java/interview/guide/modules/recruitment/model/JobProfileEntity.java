package interview.guide.modules.recruitment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recruitment_job_profiles")
@Getter
@Setter
@NoArgsConstructor
public class JobProfileEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_post_id", nullable = false, unique = true)
  private JobPostEntity jobPost;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_post_version_id", nullable = false)
  private JobPostVersionEntity jobPostVersion;

  @Column(columnDefinition = "TEXT")
  private String hardRequirementsJson;

  @Column(columnDefinition = "TEXT")
  private String competenciesJson;

  @Column(columnDefinition = "TEXT")
  private String bonusItemsJson;

  @Column(columnDefinition = "TEXT")
  private String eliminationItemsJson;

  @Column(columnDefinition = "TEXT")
  private String verificationPointsJson;

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
}
