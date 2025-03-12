package victor.training.performance.profiling;

import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService /*extends NeverDoThis*/ {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;
  private final MeterRegistry meterRegistry;
  private final ThreadPoolTaskExecutor myExecutor;

  @SneakyThrows
  public LoanApplicationDto getLoanApplication(Long loanId) {
    log.info("LoanX: {}", loanId); // Fix#1

    Future<List<CommentDto>> futureComments = myExecutor.submit(() -> getFetchComments(loanId));

    LoanApplication loan = loanApplicationRepo.findByIdLoadingSteps(loanId);
    return new LoanApplicationDto(loan, futureComments.get());
  }

  private List<CommentDto> getFetchComments(Long loanId) {
    log.info("IN fetch comments");
    return commentsApiClient.fetchComments(loanId);
  }
// parallel on Mar 12 at 3:34 I got 47 ms
// sequential on Mar 12 at 3:34 I got 54 ms

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final LastRecentlyUsed lastRecentlyUsed = new LastRecentlyUsed();

  //  @Transactional // crime to combine with synchronized. not even needed here, as i only SELECT
  public Status getLoanStatus(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findById(loanId).orElseThrow();
    lastRecentlyUsed.updateLastUsed(loanId);
    return loanApplication.getCurrentStatus(); // 5%
  }

  private final ThreadPoolTaskExecutor executor;

  public List<Long> getRecentLoanStatusQueried() {
    log.info("In parent thread");
    CompletableFuture.runAsync(() -> log.info("In a child thread"), executor).join();
    return lastRecentlyUsed.get();
  }

}
