package victor.training.performance.profiling;

import io.micrometer.core.annotation.Timed;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;
  private final MeterRegistry meterRegistry;

  public LoanApplicationDto getLoanApplication(Long loanId) {
    // recommended way to measure method execution time without aspects
    // measure total,max duration and number of calls
    List<CommentDto> comments = meterRegistry.timer("commentsapi2").record(() ->
        localMethodIsNotCalledViaAspects(loanId));

    // adds up totals
    meterRegistry.counter("moneyearnedtoday").increment(1000);

    // sample an instantaneous value: eg: concurrent number of player/calls, size of queues
    meterRegistry.gauge("instantaneousnumberofcalls", 5); // PUSH: there are 5 right now
//    meterRegistry.gauge("instantaneousnumberofcalls", () -> getTheValue()); // PULL: you attach a lambda to run on demand

//    long t0 = currentTimeMillis();
//    List<CommentDto> comments;
//    try {
//      comments = commentsApiClient.fetchComments(loanId); // takes ±40ms
//    } finally {
//      long t1 = currentTimeMillis(); // use when t1 and t0 are sampled in different threads
//      meterRegistry.timer("commentsapi").record(t1 - t0, TimeUnit.MILLISECONDS);
//    }

    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId);
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
    log.trace("Loan app: " + loanApplication);
    return dto;
  }

  @Timed
  private List<CommentDto> localMethodIsNotCalledViaAspects(Long loanId) {
    return commentsApiClient.fetchComments(loanId);
  }

  private final AuditRepo auditRepo;

  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

  public synchronized Status getLoanStatus(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findById(loanId).orElseThrow();
    recentLoanStatusQueried.remove(loanId); // BUG#7235 - avoid duplicates in list
    recentLoanStatusQueried.add(loanId);
    while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    return loanApplication.getCurrentStatus();
  }

  private final ThreadPoolTaskExecutor executor;

  public List<Long> getRecentLoanStatusQueried() {
    log.info("In parent thread");
    CompletableFuture.runAsync(() -> log.info("In a child thread"), executor).join();
    return new ArrayList<>(recentLoanStatusQueried);
  }

}
