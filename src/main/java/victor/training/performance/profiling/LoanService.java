package victor.training.performance.profiling;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import victor.training.performance.profiling.dto.CommentDto;
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
@RequiredArgsConstructor
@Transactional
public class LoanService {
  private final LoanRepo loanRepo;
  private final CommentsApiClient commentsApiClient;
  private final MeterRegistry meterRegistry;

  public LoanDto getLoanApplication(Long loanId) {
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId);
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
