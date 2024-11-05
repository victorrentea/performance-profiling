package victor.training.performance.profiling;

import ch.qos.logback.classic.Logger;
import com.zaxxer.hikari.HikariDataSource;
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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
//@Transactional // or @TransactionAttribute (JEE)
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;
  private final HikariDataSource dataSource;

  // 1) Avoid doing API calls (REST/SOAP/COBOL) while holding a DB transaction/connection
  // because connections are a scarce precious resource
  @SneakyThrows
  // @Transactional // I don't really need a Tx here since I'm just SELECTing
  public LoanApplicationDto getLoanApplication(Long loanId) {
    log.info("Start");
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId);  // 56% XXXXXXX takes ±40ms in prod
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId);// 44% XXX
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
    // if (log.isTraceEnabled()) // THE HALLMARK OF LEGACY CODE
    // Today it's generally enough to do use {} in log statements:
//    log.trace(()->"Loan app: " + loanApplication);
//    log.atDebug().addArgument(()->toJson(loanApplication)).log("Loan app: {}");
//    log.debug("Loan app: {}", ()->toJson(loanApplication));
    log.trace("Loan app: {}", loanApplication); // 20%: loanApplpication.toString is generated by Lombok
//    log.trace("Loan app: {}", ()->jsonify(loanApplication)); // 20%: loanApplpication.toString is generated by Lombok
    // to include all its fields. Some are collections that have to be LAZY-LOADED just for the toString!!!!!!!
    // Fun fact: I don't even see this log since it's on TRACE level
    return dto;
  }

  private final AuditRepo auditRepo;

  @Transactional // needed here💖
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  // a) never use Vector, it's bad performance on large sizes
  // b) instead use:
//  private final List<Long> recentLoanStatusQueried = Collections.synchronizedList(new ArrayList<>());

  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

  public synchronized void deadlock() {
    synchronized (this) { /*code*/ } // the same as 'synchronized' on the instance method
    // requires a conn:
    var a = loanApplicationRepo.findById(1L).orElseThrow();
  }

  // 2) don't combine @Transactional (keeping connections open) with synchronized
  // wasteful(JDBC pool starvation) and risky (deadlocks)
  // @Transactional // makes a magic proxy acquire 1 connection from JDBC pool






  public Status getLoanStatus(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findById(loanId).orElseThrow(); //50%
    synchronized (this) { // 35%
      recentLoanStatusQueried.remove(loanId); // BUG#7235 - avoid duplicates in list // 5%
      recentLoanStatusQueried.add(loanId);
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    }
    return loanApplication.getCurrentStatus(); // 7%
  }







  private final ThreadPoolTaskExecutor executor;

  @Transactional
  public List<Long> getRecentLoanStatusQueried() {
    log.info("In parent thread");
    CompletableFuture.runAsync(() -> log.info("In a child thread"), executor).join();
    synchronized (recentLoanStatusQueried) {
      return new ArrayList<>(recentLoanStatusQueried);
    }
  }

}
