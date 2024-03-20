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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;

  public LoanApplicationDto getLoanApplication(Long loanId) {
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // takes ±40ms in prod 50%
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // 40%
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
    log.trace("Loan app: {}", loanApplication); // 10%
    return dto;
  }

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

  public Status getLoanApplicationStatusForClient(Long id) {
    LoanApplication loanApplication = loanApplicationRepo.findById(id).orElseThrow();
    synchronized (recentLoanStatusQueried) {
      recentLoanStatusQueried.remove(id); // BUG#7235 - avoid duplicates in list
      recentLoanStatusQueried.add(id);
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    }
    return loanApplication.getCurrentStatus();
    // consider extracting a thread-safe 'BoundedQueue' class
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
    List<Long> list30 = paymentRepo.allIds(); // size = 30.000
    HashSet<Long> hashSet29 = new HashSet<>(newPaymentIds); // size = 29K (less data) or 31.000 (more data)
    hashSet29.removeAll(list30); // expected time = O(N=30K) as hashSet.remove() is O(1)
    return hashSet29.size();
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
