package interview.guide.modules.recruitment.repository;

import interview.guide.modules.recruitment.model.CandidateMessageConversationEntity;
import interview.guide.modules.recruitment.model.CandidateMessageConversationMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateMessageConversationMessageRepository
    extends JpaRepository<CandidateMessageConversationMessageEntity, Long> {

  List<CandidateMessageConversationMessageEntity> findByConversationOrderByMessageOrderAsc(
      CandidateMessageConversationEntity conversation);

  long countByConversation(CandidateMessageConversationEntity conversation);
}
