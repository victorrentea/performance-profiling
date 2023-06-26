package victor.training.performance.profiling.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import victor.training.performance.profiling.entity.LoanApplication;

public interface LoanApplicationRepo extends JpaRepository<LoanApplication, Long> {
  @Query("SELECT la FROM LoanApplication la LEFT JOIN FETCH la.steps WHERE la.id = ?1") // #2 explicit fetch din query
  LoanApplication findByIdLoadingSteps(Long id);
  // exceptie nu NULL din orice met din Spring Data Repo generata din interfata

}
