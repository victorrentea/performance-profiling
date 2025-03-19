package victor.training.performance.profiling;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;

  public LoanApplicationDto getLoanApplication(Long loanId) {
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // long and less certain 35%
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // less 50% due to JDBC Conn Pool Starvation
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);

//    log.trace("Loan app: " + loanApplication); // 15% due to Lazy Loading of Hibernate
    log.trace("Loan app: {}", loanApplication);//💖best
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
