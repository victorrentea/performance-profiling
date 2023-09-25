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
import java.util.*;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;
  private final AuditRepo auditRepo;
  // imagine a FixedSizeSyncQueue<T> < yur class new FixedSizeSyncQueue(10); //
// implementing internally synchronization
  private final List<Long> recentLoanStatusQueried = new ArrayList<>();
  private final PaymentRepo paymentRepo;
  //<editor-fold desc="insert initial payments in DB">
  private final EntityManager entityManager;
//  private final Set<Long> lukasz = Collections.synchronizedSet(new LinkedHashSet<>());

  //  @Transactional // a proxy running in front of this method
  // acquires a connfrom JDBC conn pool and keeps it blocked for the ENTIRE DURATION OF THIS METHOD!
  public LoanApplicationDto getLoanApplication(Long loanId) {
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // 75%  LONGEST i expect HTTP takes ±40ms in prod
    // open-in-view by default spring Boot keeps the JDBC connection on the HTTP thread until the response is sent to the client
    // TODO move it back first + set the property to false
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // 25%
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);

    // NEVER use if just to avoid a + => use {}
//    if (log.isTraceEnabled()) {
//      log.trace("Loan app: " + loanApplication);
////      log.trace("Loan app: {}", toJson(loanApplication)); // the only reason to use if(log.istrace0
//    }
    log.trace("Loan app: {}", loanApplication); // 8% // toString triggers a Lazy Loading of a collection of children
    // {} avoids calling a heavy toString (in this HORROR scenario, hitting the DB)!!!)
    return dto;
  }

  @Transactional // required since I do 2 x INSERT/ UPDATE..  ATOMIC
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  // 'synchronized' is implemented with C++
  // Java 21 is OUT, its best feature: Virtual Threads. they HATE 'synchronized'
//  @Transactional(isolation = ) // some fun after lunch
  public /*synchronized*/ Status getLoanApplicationStatusForClient(Long id) {
//    ReentrantLock is Virtual-Thread friendly alternative to synchronized
    LoanApplication loanApplication = loanApplicationRepo.findById(id).orElseThrow();
    synchronized (this) {// keep your critical sections (parts that have to run 1 th at a time) as small as possible
      recentLoanStatusQueried.remove(id); // BUG#7235 - avoid duplicates in list
      recentLoanStatusQueried.add(id);
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    }
    return loanApplication.getCurrentStatus();
  }
  //</editor-fold>

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

  public int getUnprocessedPayments(List<Long> newPaymentIds) {
    HashSet<Long> hashSet = new HashSet<>(newPaymentIds); // size = variable 29k, 31k (less data) or 32.000 (more data)
    Set<Long> dbPaymentIds = paymentRepo.allIds(); // size = 30.000
    hashSet.removeAll(dbPaymentIds); // expected time = O(N=30K) as hashSet.remove() is O(1)
    return hashSet.size();
  }

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
