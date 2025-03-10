package victor.training.performance.profiling;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService /*extends NeverDoThis*/ {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;
  private final MeterRegistry meterRegistry;

  // @Transactional // i only READ data. no need for ACID features here.
  public LoanApplicationDto getLoanApplication(Long loanId) {
    List<CommentDto> comments = meterRegistry.timer("commentsapi2").record(() ->
        commentsApiClient.fetchComments(loanId)); // 45%

    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // 55%
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);

//    log.trace("Loan app: " + loanApplication); // can cause lazy loading with a poor impl of toString

//    log.trace("Loan app: {}", ()-> jsonify(loanApplication)); // Avoid

//    if (log.isTraceEnabled()) { // if (trace) only if you call an expensive method while formatting the string
//      log.trace("Loan app: {}", jsonify(loanApplication)); // 13% time
//    }

    log.trace("Loan app: {}", loanApplication); // Fix#1
    return dto;
  }

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

  //  @Transactional // crime to combine with synchronized. not even needed here, as i only SELECT
  public Status getLoanStatus(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findById(loanId).orElseThrow();
    synchronized (recentLoanStatusQueried) {
      recentLoanStatusQueried.remove(loanId); // BUG#7235 - avoid duplicates in list
      recentLoanStatusQueried.add(loanId);
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    }
    return loanApplication.getCurrentStatus(); // 5%
  }

  public void evil() {
    synchronized (recentLoanStatusQueried) {
      recentLoanStatusQueried.add(1L); // other thread(s) doing this cannot RACE with getLoanStatus to change the list
    }
  }

  private final ThreadPoolTaskExecutor executor;

  @Transactional
  public List<Long> getRecentLoanStatusQueried() {
    log.info("In parent thread");
    CompletableFuture.runAsync(() -> log.info("In a child thread"), executor).join();
    return new ArrayList<>(recentLoanStatusQueried);
  }

}
