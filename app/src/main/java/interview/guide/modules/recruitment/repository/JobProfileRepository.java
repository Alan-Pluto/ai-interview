package interview.guide.modules.recruitment.repository;

import interview.guide.modules.recruitment.model.JobPostEntity;
import interview.guide.modules.recruitment.model.JobProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobProfileRepository extends JpaRepository<JobProfileEntity, Long> {

  Optional<JobProfileEntity> findByJobPost(JobPostEntity jobPost);
}
