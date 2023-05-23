package victor.training.performance.profiling.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import victor.training.performance.profiling.entity.Audit;

public interface AuditRepo extends JpaRepository<Audit, Long> {
}
