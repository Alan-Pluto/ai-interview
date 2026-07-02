package interview.guide.modules.recruitment.repository;

import interview.guide.modules.recruitment.model.JobConversationEntity;
import interview.guide.modules.recruitment.model.JobConversationMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobConversationMessageRepository
    extends JpaRepository<JobConversationMessageEntity, Long> {

  List<JobConversationMessageEntity> findByConversationOrderByMessageOrderAsc(
      JobConversationEntity conversation);

  long countByConversation(JobConversationEntity conversation);
}
