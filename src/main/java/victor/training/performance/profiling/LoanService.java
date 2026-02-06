package victor.training.performance.profiling;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import victor.training.performance.profiling.dto.LoanDto;
import victor.training.performance.profiling.entity.Audit;
import victor.training.performance.profiling.entity.Loan;
import victor.training.performance.profiling.entity.Loan.ApprovalStep.Status;
import victor.training.performance.profiling.repo.AuditRepo;
import victor.training.performance.profiling.repo.LoanRepo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoanService {
  private final LoanRepo loanRepo;
  private final CommentsApiClient commentsApiClient;

  public LoanDto getLoanApplication(Long loanId) {
    var comments = commentsApiClient.fetchComments(loanId);
    Loan loan = loanRepo.findByIdLoadingSteps(loanId);
    LoanDto dto = new LoanDto(loan, comments);
    log.trace("Return loan: " + loan);
    return dto;
  }

  private final AuditRepo auditRepo;

  public void saveLoanApplication(String title) {
    Long id = loanRepo.save(new Loan().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final LinkedHashSet<Long> recentLoanIds = new LinkedHashSet<>();
  private final MeterRegistry meterRegistry;

  @PostConstruct
  public void atStartup() {
    // TODO register a gauge metric that tracks the size of the recentLoanIds list in real-time
  }

  public synchronized Status getLoanStatus(Long loanId) {
    // TODO register a counter metric that counts how many times the status of a loan is requested, with a tag for the loanId

    Loan loan = loanRepo.findById(loanId).orElseThrow();

    recentLoanIds.remove(loanId);
    recentLoanIds.add(loanId);
    if (recentLoanIds.size() > 10) {
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
