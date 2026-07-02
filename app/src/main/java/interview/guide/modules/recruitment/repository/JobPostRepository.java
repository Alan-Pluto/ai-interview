package interview.guide.modules.recruitment.repository;

import interview.guide.modules.recruitment.model.JobPostEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostRepository extends JpaRepository<JobPostEntity, Long> {

  List<JobPostEntity> findAllByOrderByUpdatedAtDesc();
}
