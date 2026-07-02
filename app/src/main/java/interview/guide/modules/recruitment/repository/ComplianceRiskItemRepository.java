package interview.guide.modules.recruitment.repository;

import interview.guide.modules.recruitment.model.ComplianceRiskItemEntity;
import interview.guide.modules.recruitment.model.JobPostVersionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplianceRiskItemRepository extends JpaRepository<ComplianceRiskItemEntity, Long> {

  List<ComplianceRiskItemEntity> findByJobPostVersionOrderBySortOrderAsc(
      JobPostVersionEntity version);

  void deleteByJobPostVersion(JobPostVersionEntity version);
}
