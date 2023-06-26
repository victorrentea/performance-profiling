package victor.training.performance.profiling;

import io.micrometer.core.annotation.Timed;
import kotlin.jvm.internal.SerializedIr;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

import javax.persistence.EntityManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;
@Retention(RetentionPolicy.RUNTIME)
@Service
@interface Facade {

}

//@Facade

@Service
@Slf4j
//@Transactional // OK pt inceputul unei app daca ai putini useri pe app (pt 100 backoffishi)
@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;

  // de ce oare @Transactional pe GET
//  @Transactional(isolation = Isolation.REPEATABLE_READ)
//  @Transactional(isolation = Isolation.SERIALIZABLE)
  @Timed(percentiles = {.9,.99, .5}) // @Transactional @PreAuthorized...
  public LoanApplicationDto getLoanApplication(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // 15% DA, NU: 60%
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); //85% DA, NU: 37% takes ±40ms in prod =  SOAP/REST API call
    // niciodata nu faci API calls in metode @Transactional. de ce?
    //  1) oricum nu se tranzacteaza API call in sine (nu ajuta)
    //  2) performance hit: pe timpul API callului e blocata o tranzactie in DB pe 1/10 conn JDBC a
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);

//    if (log.isTraceEnabled()) { // Reject la PR: foloseste mustati ?!?!
//      log.trace("Loan app: " + loanApplication);
//    }
    log.trace("Loan app: {}", loanApplication);// better, eviti toString
//    Slf4j?

    return dto;
  }
//    if (log.isTraceEnabled()) { // Reject la PR: foloseste mustati ?!?!
//      log.trace("Loan app: {}", prettyJsonify(loanApplication));
//    }

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  // nu ti-e rusine sa tii date cross-request intr-un camp de singleton!??
  // ce-o sa zice k8s? pod++ => daca o scalez orizontal, tre s-o mut in vreun redis/DB ceva...
  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

//  @Transactional
  public  Status getLoanApplicationStatusForClient(Long id) {
    // aici: iti da Repo optional :)
//    LoanApplication loanApplication = loanApplicationRepo.findById(id).orElseThrow();
    // aici: iti de Repo un null < NU multumita package-info.java
    // aici: Repo da exceptie daca nu e dupa ID
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(id);

    log.info("In threadul original");
    CompletableFuture.runAsync(() -> {
      log.info("Niste logica care a pierdut TraceID pentru ca a fost executata pe un thread pool ne-instrumentat de spring/DI");
    }, executor); // acum se ocupa Thread Poolul springului sa propage automat TraceId catre noul thread

    // zona critica (cat mai MICA) ----
    synchronized (loanApplicationRepo) {
      recentLoanStatusQueried.remove(id); // BUG#7235 - avoid duplicates in list
      recentLoanStatusQueried.add(id);
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    }
    // -----------
    return loanApplication.getCurrentStatus();
  }

  @Autowired
  private ThreadPoolTaskExecutor executor;

  //  @Transactional
  public List<Long> getRecentLoanStatusQueried() {
    return new ArrayList<>(recentLoanStatusQueried);
  }

  //<editor-fold desc="insert initial loans in database">
  @EventListener(ApplicationStartedEvent.class)
//  @Transactional
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
    HashSet<Long> hashSet = new HashSet<>(newPaymentIds); // size = 29.999
    List<Long> dbPaymentIds = paymentRepo.allIds(); // size = 30.000
    hashSet.removeAll(new HashSet<>(dbPaymentIds)); // expected time = O(N=30K) as hashSet.remove() is O(1)
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
