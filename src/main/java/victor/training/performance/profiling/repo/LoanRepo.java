package victor.training.performance.profiling.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import victor.training.performance.profiling.entity.Loan;

public interface LoanRepo extends JpaRepository<Loan, Long> {
  @Query("SELECT la FROM Loan la LEFT JOIN FETCH la.steps")
  Loan findByIdLoadingSteps(Long id);
}
