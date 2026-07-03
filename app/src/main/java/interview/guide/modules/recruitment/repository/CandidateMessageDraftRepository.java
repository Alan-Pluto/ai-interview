package interview.guide.modules.recruitment.repository;

import interview.guide.modules.recruitment.model.CandidateMessageDraftEntity;
import interview.guide.modules.recruitment.model.CandidateJobMatchEntity;
import interview.guide.modules.recruitment.model.InterviewGuideEntity;
import interview.guide.modules.recruitment.model.JobPostEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateMessageDraftRepository extends JpaRepository<CandidateMessageDraftEntity, Long> {

  List<CandidateMessageDraftEntity> findByJobPostOrderByUpdatedAtDesc(JobPostEntity jobPost);

  List<CandidateMessageDraftEntity> findAllByOrderByUpdatedAtDesc();

  List<CandidateMessageDraftEntity> findByMatch(CandidateJobMatchEntity match);

  List<CandidateMessageDraftEntity> findByInterviewGuide(InterviewGuideEntity interviewGuide);
}
