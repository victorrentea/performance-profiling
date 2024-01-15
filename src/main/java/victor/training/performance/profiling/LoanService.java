package victor.training.performance.profiling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
//@Transactional // 54% din timp se pierde de intainte de a instra in getLoanApplication
@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;

  public LoanApplicationDto getLoanApplication(Long loanId) {
    // BAD PRACTICE: intr-o metoda @Transactional sa faci un REST API CALL duce la JDBC Connection Pool Starvation
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // 81% takes ±40ms in prod
    // move this line first for x-fun
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // 19%
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
    log.trace("Loan app: " + loanApplication);
//    log.trace("Loan app: {}", loanApplication);
    // #2 ORM face lazy load de colectii pt toString generat de Lombok @Data
    // #1 SURPRIZE: logul in prod e pe INFO nu pe TRACE
    return dto;
  }

  // avem acum in fata un JDBC Connection Pool Starvation
  // (cand termini treaba cu DB, reciclezi connex pt alt request)
  // max size =10 (HikariCP default)

  // anormal sa ai connection acquisition time mare. (normal ff aproape de 0),

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  // max 10 items
  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

  // 90% din timpul metodei asteia se sta la rand sa intre in metoda threadu, ca e altu inauntru
  // synchronized e cod C++ !!! (PANICA MARE IN JAVA21 Virtual Threads) => JFR vede alb
  // poblema e = lock contention
  // synchronized pe metoda de instanta foloseste lockul instantei (=singleton de Spring @Service)
//  @Transactional = crima daca combini cu synchronize
  public /*synchronized*/ Status getLoanApplicationStatusForClient(Long id) {
    log.info("Soc!");// ASTA!>??!?! n-are cum
    LoanApplication loanApplication = loanApplicationRepo.findById(id).orElseThrow(); // 100% din timp
    synchronized (this) { // Fix: redus dimensiunea "blocului critic"
      recentLoanStatusQueried.remove(id); // BUG#7235 - avoid duplicates in list
      recentLoanStatusQueried.add(id); // modificam o lista globala doar intr-un bloc syncronized
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    }
    return loanApplication.getCurrentStatus();
  }

  // also consider encapsulating multithreaded code in a BoundedQueue class
  public List<Long> getRecentLoanStatusQueried() {
    synchronized (this) {
      return new ArrayList<>(recentLoanStatusQueried); // intorc o copie, nu colectia aflata sub modificar concurente
      // citesc acum in pace, fara grija ca alt thread imi strica colectia pe sub
    }
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
    List<Long> list = paymentRepo.allIds(); // size = 30.000
    HashSet<Long> hashSet = new HashSet<>(newPaymentIds); // size = 29K (less data) or 31.000 (more data)
    hashSet.removeAll(list); // expected time = O(N=30K) as hashSet.remove() is O(1)
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
