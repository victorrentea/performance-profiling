package victor.training.performance.profile.showcase;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;

@Slf4j
@RestController
@RequestMapping("profile/showcase")
@RequiredArgsConstructor
public class PaymentsController {
  private final PaymentRepo paymentRepo;

  @PostMapping("payments/delta")
  public int getUnprocessedPayments(@RequestBody List<Long> newPaymentIds) {
    HashSet<Long> hashSet = new HashSet<>(newPaymentIds); // 29.999
    List<Long> dbPaymentIds = paymentRepo.allIds(); // 30.000
    hashSet.removeAll(dbPaymentIds); // should work in O(N) as set.remove() is O(1)
    return hashSet.size();
  }

  //<editor-fold desc="initial payments">
  private final EntityManager em;
  @EventListener(ApplicationStartedEvent.class)
  @Transactional
  public void initPayments() {
    log.info("Persisting payments...");
    List<Long> dbData = LongStream.rangeClosed(1, 30_000).boxed().collect(toList());
    Collections.shuffle(dbData);
    dbData.stream().map(i -> new Payment().setId(i)).forEach(em::persist);
    log.info("DONE");
  }
  //</editor-fold>
}
