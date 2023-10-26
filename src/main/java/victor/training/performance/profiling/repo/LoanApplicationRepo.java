package victor.training.performance.profiling.repo;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import victor.training.performance.profiling.entity.LoanApplication;

public interface LoanApplicationRepo extends JpaRepository<LoanApplication, Long> {
  @Timed
  @Query("SELECT la FROM LoanApplication la LEFT JOIN FETCH la.steps")
  LoanApplication findByIdLoadingSteps(Long id);

}
