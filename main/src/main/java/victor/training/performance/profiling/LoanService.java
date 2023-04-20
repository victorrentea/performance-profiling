package victor.training.performance.profiling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import victor.training.performance.profiling.LoanApplication.ApprovalStep;
import victor.training.performance.profiling.LoanApplication.Status;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;

  @Transactional(readOnly = true)
  public LoanApplicationDto getLoanApplication(Long id) {
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(id); // 15%  3-4ms
    List<CommentDto> comments = commentsApiClient.getCommentsForLoanApplication(id); // 85% takes ±40ms
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
    log.trace("Loan app: {}", loanApplication);
    return dto;
  }

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

  public synchronized Status getLoanApplicationStatusForClient(Long id) {
    LoanApplication loanApplication = loanApplicationRepo.findById(id).orElseThrow();
    recentLoanStatusQueried.remove(id); // BUG#7235 - avoid duplicates in list
    recentLoanStatusQueried.add(id);
    while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    return loanApplication.getCurrentStatus();
  }

  public List<Long> getRecentLoanStatusQueried() {
    return new ArrayList<>(recentLoanStatusQueried);
  }

  //<editor-fold desc="Initial Data">
  @EventListener(ApplicationStartedEvent.class)
  public void insertInitialData() {
    ApprovalStep step1 = new ApprovalStep().setName("Pre-Scan Client").setStatus(Status.APPROVED);
    ApprovalStep step2 = new ApprovalStep().setName("Credit Registry").setStatus(Status.DECLINED);
    loanApplicationRepo.save(new LoanApplication()
            .setId(1L)
            .setTitle("4Porche")
            .setSteps(List.of(step1, step2)));
  }
  //</editor-fold>
}
