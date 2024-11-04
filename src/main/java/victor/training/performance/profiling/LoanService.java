package victor.training.performance.profiling;

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
    Connection connection = dataSource.getConnection();
    connection.setAutoCommit(false); // = start tx
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // takes ±40ms in prod
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId);
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
    log.trace("Loan app: " + loanApplication);
    connection.commit();
    return dto;
  }





  private final AuditRepo auditRepo;

  @Transactional // needed here💖
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

  @Transactional
  public synchronized Status getLoanStatus(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findById(loanId).orElseThrow();
    recentLoanStatusQueried.remove(loanId); // BUG#7235 - avoid duplicates in list
    recentLoanStatusQueried.add(loanId);
    while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    return loanApplication.getCurrentStatus();
  }

  private final ThreadPoolTaskExecutor executor;

  @Transactional
  public List<Long> getRecentLoanStatusQueried() {
    log.info("In parent thread");
    CompletableFuture.runAsync(() -> log.info("In a child thread"), executor).join();
    return new ArrayList<>(recentLoanStatusQueried);
  }

}
