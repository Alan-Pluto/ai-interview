package interview.guide.modules.recruitment.repository;

import interview.guide.modules.recruitment.model.JobConversationEntity;
import interview.guide.modules.recruitment.model.JobPostEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobConversationRepository extends JpaRepository<JobConversationEntity, Long> {

  List<JobConversationEntity> findByJobPostOrderByUpdatedAtDesc(JobPostEntity jobPost);

  Optional<JobConversationEntity> findFirstByJobPostOrderByUpdatedAtDesc(JobPostEntity jobPost);
}
