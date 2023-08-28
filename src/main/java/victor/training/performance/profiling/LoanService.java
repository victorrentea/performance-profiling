package victor.training.performance.profiling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
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
import java.util.*;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
//@Transactional
@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;

//  @Transactional
  public LoanApplicationDto getLoanApplication(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // 25% move this line first for x-fun
    // by default Spring odata ce a obtinut connex JDBC pe un thread de web, va tine acea conexiune
    // pana la trimiterea raspunsului HTTP!
    // => rezultat: tin 1/10 JDBC conn blocate cat timp fac requestul HTTP de mai jos !!
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // 75% takes ±40ms in prod
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
    log.trace("Loan app: {}", loanApplication);
    return dto;
  }

  private final AuditRepo auditRepo;

  @Transactional // necesar ca am 2 DB insert
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
    // 1) old-age: DB trigger
    // Envers merita investigat !
    // 2) new-age: CDC cu Debezium/Kafka Connect care tailuieste logul tranzactiilor din DB https://debezium.io/documentation/faq/
    //  si imediat ce vede un INSERT nou in LOAN_APPLICATION trimite automat un mesaj pe Kafka
         // in loc de auditRepo.save aveai kafkaSender.send(
    // 3) Event-Sourcing (stochezi doar eventurile si reconstruiesti restul din trailul de eventuri)
  }


  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

//  @Transactional
  public synchronized Status getLoanApplicationStatusForClient(Long id) {
    LoanApplication loanApplication = loanApplicationRepo.findById(id).orElseThrow();
    recentLoanStatusQueried.remove(id); // BUG#7235 - avoid duplicates in list
    recentLoanStatusQueried.add(id);
    while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    return loanApplication.getCurrentStatus();
  }

//  @Transactional
  public List<Long> getRecentLoanStatusQueried() {
    return new ArrayList<>(recentLoanStatusQueried);
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
    List<Long> dbPaymentIds = paymentRepo.allIds(); // size = 30.000
    hashSet.removeAll(dbPaymentIds); // expected time = O(N=30K) as hashSet.remove() is O(1)
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
