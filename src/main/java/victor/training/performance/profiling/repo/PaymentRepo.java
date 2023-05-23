package victor.training.performance.profiling.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import victor.training.performance.profiling.entity.Payment;

import java.util.List;

public interface PaymentRepo extends JpaRepository<Payment, Long> {
  @Query("SELECT id FROM Payment")
  List<Long> allIds();
}
