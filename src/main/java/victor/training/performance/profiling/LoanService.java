package victor.training.performance.profiling;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.System.currentTimeMillis;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;
  private final MeterRegistry meterRegistry;

  public LoanApplicationDto getLoanApplication(Long loanId) {
    // TODO use JFR Events to monitor waiting time in queue of a thread pool
    // threadPool.submit(() -> work());
    List<CommentDto> comments =
        meterRegistry.timer("fetch_comments_from_api").record(() -> // #2 FP style
            commentsApiClient.fetchComments(loanId) // long and less certain 35%
        );

    if (comments.isEmpty()) {
      meterRegistry.counter("loan_without_comments").increment();
    }
    Timer timer = meterRegistry.timer("find_loan_sql");
    long t0 = currentTimeMillis(); // #1 high-school : avoid
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId);
    long t1 = currentTimeMillis(); // use only if this is collected in another thead (callback-style)
    timer.record(Duration.ofMillis(t1 - t0));

    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);

    log.trace("Loan app: " + loanApplication); // 15% due to Lazy Loading of Hibernate
//    log.trace("Loan app: {}", loanApplication);//💖best
//    log.trace("Loan app: {}", jsonify(loanApplication));//BAD call to jsonify still gets called
//    if (log.isTraceEnabled()) log.trace("Loan app: {}", jsonify(loanApplication));
//    //Logger.getLogger().log(()->"print me");
    return dto;
  }

  private String jsonify(LoanApplication loanApplication) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return objectMapper.writeValueAsString(loanApplication);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

  // called by 1 of the 200 *(default) threads of Tomcat
  public Status getLoanStatus(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findById(loanId).orElseThrow(); // high latency
    synchronized (this) { // ALWAYS shrink the critical section to the minimum
      recentLoanStatusQueried.remove(loanId); // BUG#7235 - avoid duplicates in list
      recentLoanStatusQueried.add(loanId);
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    }
    return loanApplication.getCurrentStatus();
  }

  private final ThreadPoolTaskExecutor executor;

  public List<Long> getRecentLoanStatusQueried() {
    log.info("In parent thread");
    CompletableFuture.runAsync(() -> log.info("In a child thread"), executor).join();
    synchronized (this) {
      return new ArrayList<>(recentLoanStatusQueried);
    }
  }

}
