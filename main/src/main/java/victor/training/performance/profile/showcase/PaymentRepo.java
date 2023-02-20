package victor.training.performance.profile.showcase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentRepo extends JpaRepository<Payment, Long> {
  @Query("SELECT id FROM Payment")
  List<Long> allIds();
}
