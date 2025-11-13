package victor.training.performance.profiling;

import io.micrometer.observation.annotation.Observed;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import victor.training.performance.profiling.dto.CommentDto;
import victor.training.performance.profiling.dto.LoanDto;
import victor.training.performance.profiling.entity.Audit;
import victor.training.performance.profiling.entity.Loan;
import victor.training.performance.profiling.entity.Loan.Status;
import victor.training.performance.profiling.repo.AuditRepo;
import victor.training.performance.profiling.repo.LoanRepo;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {
  private final LoanRepo loanRepo;
  private final CommentsApiClient commentsApiClient;

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

  private final List<Long> recentLoanIds = new ArrayList<>();

  @PostConstruct
  public void atStartup() {
    // TODO gauge on list above
  }

  public synchronized Status getLoanStatus(Long loanId) {
    // TODO counter ++
    Loan loan = loanRepo.findById(loanId).orElseThrow();
    recentLoanIds.remove(loanId); // BUG#7235 - avoid duplicates in list
    recentLoanIds.add(loanId);
    while (recentLoanIds.size() > 10) recentLoanIds.remove(0);
    return loan.getCurrentStatus();
  }

  public List<Long> getRecentLoanIds() {
    return new ArrayList<>(recentLoanIds);
  }
}
