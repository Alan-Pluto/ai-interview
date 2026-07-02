package interview.guide.modules.recruitment.repository;

import interview.guide.modules.recruitment.model.JobPostEntity;
import interview.guide.modules.recruitment.model.JobPostVersionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostVersionRepository extends JpaRepository<JobPostVersionEntity, Long> {

  List<JobPostVersionEntity> findByJobPostOrderByVersionNoDesc(JobPostEntity jobPost);

  Optional<JobPostVersionEntity> findFirstByJobPostOrderByVersionNoDesc(JobPostEntity jobPost);
}
