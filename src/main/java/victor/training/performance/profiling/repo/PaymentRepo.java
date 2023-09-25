package victor.training.performance.profiling.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import victor.training.performance.profiling.entity.Payment;

import java.util.Set;

public interface PaymentRepo extends JpaRepository<Payment, Long> {
  @Query("SELECT id FROM Payment")
  Set<Long> allIds();
}
