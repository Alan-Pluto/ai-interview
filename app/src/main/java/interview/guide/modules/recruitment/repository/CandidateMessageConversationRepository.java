package interview.guide.modules.recruitment.repository;

import interview.guide.modules.recruitment.model.CandidateMessageConversationEntity;
import interview.guide.modules.recruitment.model.CandidateMessageDraftEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateMessageConversationRepository
    extends JpaRepository<CandidateMessageConversationEntity, Long> {

  Optional<CandidateMessageConversationEntity> findByDraft(CandidateMessageDraftEntity draft);
}
