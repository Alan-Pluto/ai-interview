package interview.guide.modules.recruitment.repository;

import interview.guide.modules.recruitment.model.InterviewGuideEntity;
import interview.guide.modules.recruitment.model.JobPostEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterviewGuideRepository extends JpaRepository<InterviewGuideEntity, Long> {

  List<InterviewGuideEntity> findByJobPostOrderByUpdatedAtDesc(JobPostEntity jobPost);
}
