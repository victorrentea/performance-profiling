package victor.training.performance.profiling;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import victor.training.performance.profiling.dto.CommentDto;
import victor.training.performance.profiling.dto.LoanDto;
import victor.training.performance.profiling.entity.Audit;
import victor.training.performance.profiling.entity.Loan;
import victor.training.performance.profiling.entity.Loan.ApprovalStep.Status;
import victor.training.performance.profiling.repo.AuditRepo;
import victor.training.performance.profiling.repo.LoanRepo;
import victor.training.performance.profiling.util.Sleep;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

@Slf4j
@Service
//@Transactional // NEVER here! DANGEROUS ☢️☢️☢️☢️ as only a fraction of methods WRITE (most read)
@RequiredArgsConstructor
public class LoanService /*exnteds BaseService*/ {
  private final LoanRepo loanRepo;
  private final CommentsApiClient commentsApiClient;
  private final ThreadPoolTaskExecutor taskExecutor;

  //  @Transactional
  public LoanDto getLoanApplication(Long loanId) {
    Timer timerMetric = meterRegistry.timer("timer_metric");
    // a) sum up parts of a flow, not all
    // b)
    long t0 = currentTimeMillis(); // http thread
    CompletableFuture.runAsync(() -> {
      System.out.println("Some bg work");
      Sleep.millis(100);
      long t1 = currentTimeMillis(); // worker thread
      timerMetric.record(t1 - t0, TimeUnit.MILLISECONDS);
    });

    var commentsCF = CompletableFuture.supplyAsync(
        ()->commentsApiClient.fetchComments(loanId), taskExecutor);

    var loan = myMethod(loanId);
    // #1🤔 On what thread pool does the work run now?
    //  => run ForkJoinPool.commonPool()
    //      .maxThreads = #CPU-1
    //      .waitingQueue in memory maxSize=INFINTE
    // ☢️ competing with any parallelStream in this JVM.
    // ❌ don't do I/O work on commonPool()
    // 😊 otel javaagent copied the traceid from parent thread to workerthreads
    // #2 I only actually need 1 extra thread
    // #3☢️ DOS risk: 😈 sends 1k request in one burst: fire many rps but close the conn
    //   if the 😈 waits for its requests to compelte => 200 max running => 3 in execution on FJP.commonPool + 197 in its q


    // by default Tomcat
    //    start 200 max threads
    //    respond with 503 if already having 500 connections

    List<CommentDto> comments = commentsCF.join();
    LoanDto dto = new LoanDto(loan, comments);
    log.trace("Return loan: {}", loan);
    return dto;
  }

  private Loan myMethod(Long loanId) {
    log.info("ON another thread");
    return loanRepo.findByIdLoadingSteps(loanId);
  }


  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanRepo.save(new Loan().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final LinkedHashSet<Long> recentLoanIds = new LinkedHashSet<>();
  private final MeterRegistry meterRegistry;

  @PostConstruct
  public void atStartup() {
    Gauge.builder("recent_loan_ids_size", recentLoanIds, LinkedHashSet::size) // pulling (callback)
        .register(meterRegistry);
    Gauge.builder("loans_total", loanRepo, LoanRepo::count) // +1 query : don't❌
        .register(meterRegistry);
  }

  public synchronized Status getLoanStatus(Long loanId) {
    meterRegistry.counter("loan_status_requests", "loanId", loanId.toString()).increment();

    Loan loan = loanRepo.findById(loanId).orElseThrow();

    recentLoanIds.remove(loanId);
    recentLoanIds.add(loanId);
    if (recentLoanIds.size() > 10) {
      meterRegistry.gauge("recent_loan_ids_size2", recentLoanIds.size()); // push
      recentLoanIds.removeFirst();
    }
    return loan.getCurrentStatus();
  }

  public synchronized List<Long> getRecentLoanIds() {
    return new ArrayList<>(recentLoanIds);
  }
}

// Tip:  to see the average value of a timer in ms, use the following promQL:
// (rate(comments_queue_waiting_time_seconds_sum[1m])/rate(comments_queue_waiting_time_seconds_count[1m]))*1000
// Hikari connection acquisition time in ms:
// (rate(hikaricp_connections_acquire_seconds_sum{pool="HikariPool-1"}[1m])/rate(hikaricp_connections_acquire_seconds_count{pool="HikariPool-1"}[1m]))*1000
