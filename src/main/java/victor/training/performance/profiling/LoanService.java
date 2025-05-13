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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;
  private final ThreadPoolTaskExecutor executor;


  public LoanApplicationDto getLoanApplication(Long loanId) throws ExecutionException, InterruptedException {
    // using CompletableFuture#..Async(,springDecoratedExecutor) !!! past pass the 2nd arg
    var futureComments = CompletableFuture.supplyAsync(
        () -> commentsApiClient.fetchComments(loanId), executor);
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // 50%
    var comments = futureComments.get();
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
    log.info("Loan app: {}", loanApplication);
    return dto;
  }
  private final AuditRepo auditRepo;


  @Transactional
  public void saveLoanApplication(String title) {
//    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // takes ±40ms
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }
  private final BoundedList<Long> recentLoanIdQueried = new BoundedList<>();

  // Redis semaphore
  // SELECT for UPDATE = row / LOCK TABLE = table

  // new Semaphore()
  // new Barrier()
  // new ReentrantLock()
  // the above DONT SHOW UP as Java Monitor locks


  public Status getLoanStatus(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findById(loanId).orElseThrow();
    recentLoanIdQueried.add(loanId);
    return loanApplication.getCurrentStatus();
  }

  public List<Long> getRecentLoanStatusQueried() {
    log.info("In parent thread");
    CompletableFuture.runAsync(() -> log.info("In a child thread"), executor).join();

//    return recentLoanStatusQueried; // wrong! later traversal (eg jackson serialization) might cause
    // ConcurrentModificationException

//    synchronized (this) {
//      return new ArrayList<>(recentLoanStatusQueried);
//    }
    return recentLoanIdQueried.getAll();
  }

}
