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


//  @Transactional (readOnly = true)// used to tell RountingDataSource to use
  // the JDBC Connec Pool optimized for reading,
  // make sure that by deault CrudRepo uses the read conn

  public LoanApplicationDto getLoanApplication(Long loanId) {
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // 10ms 40%
    // if the REST API call took 100ms, we won't see a problem in our exp. as most of our latency is spent on API call
    // above true for closed world 23
    // open world: requests/sec, the above doesnt change the problem
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // 50%
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
//    log.trace("Loan app: " + loanApplication); // 10% = JPA lazy loading
    log.trace("Loan app: {}", loanApplication); //

//    if (log.isTraceEnabled()) log.trace("Loan app: " + toJson(loanApplication)); //
//    log.atTrace().log(()->"Loan app: " + toJson(loanApplication));
    return dto;
  }

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
//    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // takes ±40ms
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final List<Long> recentLoanStatusQueried = new ArrayList<>();


  // Redis semaphore
  // SELECT for UPDATE = row / LOCK TABLE = table

  // new Semaphore()
  // new Barrier()
  // new ReentrantLock()
  // the above DONT SHOW UP as Java Monitor locks

  public Status getLoanStatus(Long loanId) {
    synchronized (this) {
      LoanApplication loanApplication = loanApplicationRepo.findById(loanId).orElseThrow();
      recentLoanStatusQueried.remove(loanId); // remove it BUG#7235 - avoid duplicates in list
      recentLoanStatusQueried.add(loanId); // to add it again at the end
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0); // ensure list size <= 10
      return loanApplication.getCurrentStatus();
    }
  }

  private final ThreadPoolTaskExecutor executor;

  public List<Long> getRecentLoanStatusQueried() {
    log.info("In parent thread");
    CompletableFuture.runAsync(() -> log.info("In a child thread"), executor).join();
    return new ArrayList<>(recentLoanStatusQueried);
  }

}
