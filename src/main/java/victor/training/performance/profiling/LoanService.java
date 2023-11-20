package victor.training.performance.profiling;

import ch.qos.logback.classic.Logger;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;
import victor.training.performance.profiling.dto.CommentDto;
import victor.training.performance.profiling.dto.LoanApplicationDto;
import victor.training.performance.profiling.entity.Audit;
import victor.training.performance.profiling.entity.LoanApplication;
import victor.training.performance.profiling.entity.LoanApplication.ApprovalStep;
import victor.training.performance.profiling.entity.LoanApplication.Status;
import victor.training.performance.profiling.entity.Payment;
import victor.training.performance.profiling.repo.AuditRepo;
import victor.training.performance.profiling.repo.LoanApplicationRepo;
import victor.training.performance.profiling.repo.PaymentRepo;

import java.util.*;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service

@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;

//  @Transactional// bloca conex JDBC pt toata durata metodei + REST Call in metoda = Connection Pool Starvation
  public LoanApplicationDto getLoanApplication(Long loanId) {
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); //  75% takes ±40ms in prod
    // move this line first for x-fun
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // 23 %
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
    log.trace("Loan app: {}", loanApplication);
    // folosim{} ca sa evitam un toString() scump (LAZY LOAD IN PUII MEI) + toString sa nu faca vreodata lazy loading
    return dto;
  }

  private final AuditRepo auditRepo;

  @Transactional // ok aici
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final BoundedQueue<Long> recentLoanStatusQueried = new BoundedQueue<>(10);
//  private final List<Long> recentLoanStatusQueried = new ArrayList<>();
//  private final BoundedQueue<OStructuraCuSetteri> aiGresitFilmu = new BoundedQueue<>(10);
  // codul multi-threaded iubeste clasele imutabile


  // 'synchronized' este implementat in JVM cu cod C++, nu Java.
  // JFR nu vede in C++.

  // REGULA: eviti sa pui synchronized pe metode din backend
  public /*synchronized*/ Status getLoanApplicationStatusForClient(Long id) {
    // syncronized instance method = 1 singur thread poate intra in aceasta metoda pt instanta curenta (singleton global)
    LoanApplication loanApplication = loanApplicationRepo.findById(id).orElseThrow();

//    recentLoanStatusQueried.getContents().get(0).setTzeapa()
    recentLoanStatusQueried.add(id);
    // Recomandare de design: daca ai date modificabile din multithread in mod thread safe,
    // => INCAPSULEZI acele date intr-o clasa noua "new BoundedQueue(10); .add"

    return loanApplication.getCurrentStatus();
  }

  public List<Long> getRecentLoanStatusQueried() {
    return recentLoanStatusQueried.getContents();
  }

  //<editor-fold desc="insert initial loans in database">
  @EventListener(ApplicationStartedEvent.class)
  public void insertInitialData() {
    ApprovalStep step1 = new ApprovalStep().setName("Pre-Scan Client").setStatus(Status.APPROVED);
    ApprovalStep step2 = new ApprovalStep().setName("Credit Registry").setStatus(Status.DECLINED);
    loanApplicationRepo.save(new LoanApplication()
        .setId(1L)
        .setTitle("4Porche")
        .setSteps(List.of(step1, step2)));
  }
  //</editor-fold>

  private final PaymentRepo paymentRepo;

  public int getUnprocessedPayments(List<Long> newPaymentIds) {
    HashSet<Long> hashSet = new HashSet<>(newPaymentIds); // size = 29.999 (less data) or 32.000 (more data)
    List<Long> list = paymentRepo.allIds(); // size = 30.000
    hashSet.removeAll(new HashSet<>(list)); // expected time = O(N=30K) as hashSet.remove() is O(1)
    return hashSet.size();
  }

  //<editor-fold desc="insert initial payments in DB">
  private final EntityManager entityManager;
  @EventListener(ApplicationStartedEvent.class)
  @Transactional //batch together the inserts
  public void initPayments() {
    log.info("Persisting payments...");
    List<Long> dbData = LongStream.rangeClosed(1, 30_000).boxed().collect(toList());
    Collections.shuffle(dbData);
    dbData.stream().map(i -> new Payment().setId(i)).forEach(entityManager::persist);
    log.info("DONE");
  }
  //</editor-fold>

}
