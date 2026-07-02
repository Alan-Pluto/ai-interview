package interview.guide.modules.recruitment.model;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recruitment_compliance_risk_items")
@Getter
@Setter
@NoArgsConstructor
public class ComplianceRiskItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_post_version_id", nullable = false)
  private JobPostVersionEntity jobPostVersion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RiskLevel riskLevel;

  @Column(nullable = false, length = 80)
  private String riskType;

  @Column(columnDefinition = "TEXT")
  private String matchedText;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String description;

  @Column(columnDefinition = "TEXT")
  private String suggestion;

  @Column(columnDefinition = "TEXT")
  private String basis;

  @Column(nullable = false)
  private Integer sortOrder = 0;

  public enum RiskLevel {
    HIGH,
    MEDIUM,
    LOW
  }
}
