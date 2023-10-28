package victor.training.performance.profiling;

import jakarta.persistence.EntityManager;
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

import java.util.*;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;

  public List<CommentDto> getLoanCommentsBis(Long loanId) {
    List<CommentDto> list1 = commentsApiClient.fetchComments(loanId);
    List<CommentDto> list2 = commentsApiClient.fetchComments(loanId); // imagine different APIs
    List<CommentDto> list3 = commentsApiClient.fetchComments(loanId); // apiC
    return Stream.concat(Stream.concat(list1.stream(), list2.stream()), list3.stream())
        .toList();
  }

  public LoanApplicationDto getLoanApplication(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // 26% > 60% not 20%
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // 70% > 30% not 80% takes ±40ms in prod
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
//    if (log.isTraceEnabled()) {
//      log.trace("Loan app: {}", toJson(loanApplication)); // 10% ARE YOU NUTS!?? it's a log trace!!
//    }
    log.trace("Loan app: {}", loanApplication); // 10% ARE YOU NUTS!?? it's a log trace!!
    return dto;
  }

  private final AuditRepo auditRepo;

  @Transactional // Atomic 2 inserts
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }
  Set<Long> set = new HashSet<>();
  // global list, because it's an instance field of a spring singleton

  private final List<Long> recentLoanStatusQueried = /*Collections.synchronizedList(*/new ArrayList<>();
//  @Transactional // crime if on a syncronized method
  // synchronized means one single thread can enter this method on this instance (singleton)

  public  Status getLoanApplicationStatusForClient(Long id) {
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(id);
    synchronized (this) {
      // critical section
      recentLoanStatusQueried.remove(id); // BUG#7235 - avoid duplicates in list
      recentLoanStatusQueried.add(id);
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    }
    // Further advice: if you want to play with mutable object in multithread env,
    // PLEASE create a dedicated class to encapsulate those changes in a thread-save OBJECT
    // playing Extreme OOP eg
    // new BoundedList(10);
    // boundendList.add() {internally syncrhronized
    return loanApplication.getCurrentStatus();
  }

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
    dbPaymentIds.forEach(hashSet::remove);
//    hashSet.removeAll(new HashSet<>(dbPaymentIds)); // 98% expected time = O(N=30K) as hashSet.remove() is O(1)
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
