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

  @SneakyThrows
  public LoanApplicationDto getLoanApplication(Long loanId) {
    log.info("LoanX: {}", loanId); // Fix#1

    ExecutorService threadPool = Executors.newFixedThreadPool(1); //1 STUPID: overhead each call
    // FATAL: I lost the traceId because I used a second thread from a thread pool which was not decorated
    //  to propagate TraceID correctly from parent thread to worker thread.

    Future<List<CommentDto>> futureComments = threadPool.submit(() -> commentsApiClient.fetchComments(loanId));

    LoanApplication loan = loanApplicationRepo.findByIdLoadingSteps(loanId);

    threadPool.shutdown(); // FATAL: leads to out of memory. pool started by a request is never closed
    return new LoanApplicationDto(loan, futureComments.get());
  }

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
