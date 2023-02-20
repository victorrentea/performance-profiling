package victor.training.performance.profile.showcase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LoanApplicationRepo extends JpaRepository<LoanApplication, Long> {
  @Query("SELECT la FROM LoanApplication la LEFT JOIN FETCH la.steps")
  LoanApplication findByIdLoadingSteps(Long id);

}
