package victor.training.performance.profiling;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Slf4j
@Service // 1 instance
@RequiredArgsConstructor
//@Transactional // jr was here / migrated from EJB @Stateless
@Observed // TODO visualize
public class LoanService /*extends BaseService*/{
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;
  private final ObservationRegistry registry;
  private final Tracer tracer;
  private final MeterRegistry meterRegistry; // micrometer

  @PostConstruct // runs once, at spring startup
  void onStartup() {
    // pull-based via callback: at any cal of
    // http://localhost:8080/actuator/prometheus is called, the -> runs
    meterRegistry.gauge("noofloans",loanApplicationRepo,
        repo->repo.count() // this -> might run every 5 sec
        //    List<CommentDto> comments = meterRegistry.timer("mytimer")
        //        .record(()-> commentsApiClient.fetchComments(loanId)); //=> _sum+=t1-t0; _count++
        //
        //    meterRegistry.counter("tissuesused").increment(100);//echo pregnant🤰
        //    meterRegistry.counter("tissuesused").increment(1);//normal eco
        ////    meterRegistry.gauge("noofloans",100);// push-based
    );
  }
  private final ThreadPoolTaskExecutor executor;
  @Timed
  public LoanApplicationDto getLoanApplication(Long loanId) throws ExecutionException, InterruptedException {
    log.debug("START");
//    var futureComments = executor.submit(() ->fetch(loanId)); // pre java8
//    var futureComments = CompletableFuture.supplyAsync(()->fetch(loanId)); // never in BE
    var futureComments = CompletableFuture.supplyAsync(()->fetch(loanId), executor); // ✅YES!
//    var futureComments = fetch(loanId); // ❌/✅YES!
    var loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId);  // 30% ~2..6ms = SELECT -> DB
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, futureComments.get());
    log.debug("Loan app: {}", loanApplication); // calls toString on params <=>level<=DEBUG
    return dto;
  }
  //@Async // NEVER USE
  private List<CommentDto> fetch(Long loanId) {
    log.debug("API CALL");
    return commentsApiClient.fetchComments(loanId);
  }

  Logger logU;

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  // last 10
  // not enough
//  private final List<Long> recentLoanStatusQueried = synchronizedList(new ArrayList<>());
  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

  public Status getLoanStatus(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findById(loanId).orElseThrow();
    synchronized (recentLoanStatusQueried) {
      recentLoanStatusQueried.remove(loanId); // BUG#7235 - avoid duplicates in list
      recentLoanStatusQueried.add(loanId);
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.removeFirst();
    }
    return loanApplication.getCurrentStatus();
  }

  public List<Long> getRecentLoanStatusQueried() {
    log.info("In parent thread");
    CompletableFuture.runAsync(() -> log.info("In a child thread"), executor).join();
    synchronized (recentLoanStatusQueried) {
      return new ArrayList<>(recentLoanStatusQueried); // protected read
    }
  }

  public void autoFlushFaraTx() {
    LoanApplication loan = loanApplicationRepo.findById(1L).orElseThrow();
    loan.setTitle("Altul");
  }

}
