package victor.training.performance.profiling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import victor.training.performance.profiling.entity.Payment;
import victor.training.performance.profiling.repo.PaymentRepo;

import javax.persistence.EntityManager;
import javax.xml.bind.JAXBContext;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentsController {
  private final PaymentRepo paymentRepo;

  @PostMapping("payments/delta")
  public int getUnprocessedPayments(@RequestBody List<Long> newPaymentIds) {
    HashSet<Long> hashSet = new HashSet<>(newPaymentIds); // size = 29.999
    List<Long> dbPaymentIds = paymentRepo.allIds(); // size = 30.000
    hashSet.removeAll(dbPaymentIds); // expected time = O(N=30K) as hashSet.remove() is O(1)
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
