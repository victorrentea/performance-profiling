package victor.training.performance.profile.showcase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import victor.training.performance.profile.showcase.LoanApplication.ApprovalStep;
import victor.training.performance.profile.showcase.LoanApplication.Status;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final AuditRepo auditRepo;
  private final CommentsApiClient commentsApiClient;
  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

  @Transactional // ai nevoie aici
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

//  @Transactional  NU ai nevoie de ACID daca citesti !!
  public LoanApplicationDto getLoanApplication(Long id) {
    List<CommentDto> comments = commentsApiClient.getCommentsForLoanApplication(id); // takes ±40ms
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(id); // #solutia 1 la JDBC starvation
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
    log.trace("Loan app: {}", loanApplication);
    return dto;
  }

//  @Transactional
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
