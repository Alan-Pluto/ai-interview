package interview.guide.modules.recruitment.repository;

import interview.guide.modules.recruitment.model.CandidateJobMatchEntity;
import interview.guide.modules.recruitment.model.JobPostEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateJobMatchRepository extends JpaRepository<CandidateJobMatchEntity, Long> {

  List<CandidateJobMatchEntity> findByJobPostOrderByCreatedAtDesc(JobPostEntity jobPost);

  List<CandidateJobMatchEntity> findAllByOrderByCreatedAtDesc();
}
