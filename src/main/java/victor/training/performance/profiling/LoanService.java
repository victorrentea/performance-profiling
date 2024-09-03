package victor.training.performance.profiling;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import victor.training.performance.profiling.dto.CommentDto;
import victor.training.performance.profiling.dto.LoanApplicationDto;
import victor.training.performance.profiling.entity.Audit;
import victor.training.performance.profiling.entity.LoanApplication;
import victor.training.performance.profiling.entity.LoanApplication.Status;
import victor.training.performance.profiling.repo.AuditRepo;
import victor.training.performance.profiling.repo.LoanApplicationRepo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;

  @SneakyThrows
//  @Transactional // too early opened and end it after the method terminates.
  // do I NEED an SQL transaction to do a SELECT? NO!
  // goal: keep less time the connection blocked
  public LoanApplicationDto getLoanApplication(Long loanId) {
    log.info("Start");
    // terrible: API call while holding a JDBC transcation/conenction open = suicide.
    // total# conn = 10 (default)
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); //  18% A: takes ±40ms in prod
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // 65%
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
    log.trace("Loan app: {}", loanApplication); // 15% - WTF!?!!!! // FIX: {}
    // Spring Boot 2. by default keeps the JDBC Connection open and bound to this thread
    // until the end of the HTTP request. WHY?
    // To avoid "Cannot initialize proxy - No session" errors, like any Jr faces.
    // fix: open-session-in-view: false
    return dto;
  }

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

//  @Transactional // FIXME Error #1 no need to transact a read
  public  Status getLoanStatus(Long loanId) { // FIXME Error #2: synchronized too much code
    LoanApplication loanApplication = loanApplicationRepo.findById(loanId).orElseThrow();

    synchronized (recentLoanStatusQueried) {
      recentLoanStatusQueried.remove(loanId); // BUG#7235 - avoid duplicates in list
      recentLoanStatusQueried.add(loanId);
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    return loanApplication.getCurrentStatus(); // hides a lazy load Hit to DB => critical section still too long
    }
  }

  private final ThreadPoolTaskExecutor executor;

  public List<Long> getRecentLoanStatusQueried() {
    log.info("In parent thread");
    CompletableFuture.runAsync(() -> log.info("In a child thread"), executor).join();
    return new ArrayList<>(recentLoanStatusQueried);
  }

}
