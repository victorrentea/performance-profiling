package victor.training.performance.profiling;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Tracer;
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
import java.util.logging.Logger;

@Slf4j
@Service
@RequiredArgsConstructor
//@Transactional // jr was here / migrated from EJB @Stateless
@Observed // TODO visualize
public class LoanService /*extends BaseService*/{
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;
  private final ObservationRegistry registry;
  private final Tracer tracer;

  public LoanApplicationDto getLoanApplication(Long loanId) {
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // 60% ~10ms--20s = REST API call
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // 16% ~2..6ms = SELECT -> DB
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
//    log.debug("Loan app: " + loanApplication); // useless toString if level=INFO
    log.debug("Loan app: {}", loanApplication); // calls toString on params <=>level<=DEBUG
//    logU.fine(()->"Loan app: " + loanApplication); // same but JCL not Slf4J
    return dto;
  }
  Logger logU;

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

  //@Transactional(readOnly = true)
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

  public void autoFlushFaraTx() {
    LoanApplication loan = loanApplicationRepo.findById(1L).orElseThrow();
    loan.setTitle("Altul");
  }

}
