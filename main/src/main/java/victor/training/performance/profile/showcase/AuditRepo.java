package victor.training.performance.profile.showcase;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepo extends JpaRepository<Audit, Long> {
}
