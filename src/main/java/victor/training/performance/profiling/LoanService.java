package victor.training.performance.profiling;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import victor.training.performance.profiling.dto.CommentDto;
import victor.training.performance.profiling.dto.LoanApplicationDto;
import victor.training.performance.profiling.entity.Audit;
import victor.training.performance.profiling.entity.LoanApplication;
import victor.training.performance.profiling.entity.LoanApplication.ApprovalStep;
import victor.training.performance.profiling.entity.LoanApplication.Status;
import victor.training.performance.profiling.repo.AuditRepo;
import victor.training.performance.profiling.repo.LoanApplicationRepo;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;
  private final MeterRegistry meterRegistry;

  public LoanApplicationDto getLoanApplication(Long loanId) {
    // A
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); //A:64%; STUPID!!!
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // A:34%;
    // B
//    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // B: 90% takes ±40ms in prod
//    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // B:10% OK

    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
//    if (log.isTraceEnabled()) { // NICIODATA asa:
//      log.trace("Loan app: " + loanApplication); // + evalueaza loanApplication.toString()
//    }
//    log.trace("Loan app: {}", loanApplication); // face .toString pe obiect doar DACA log == trace
    log.debug("Loan app: {}", loanApplication);
//    if (log.isTraceEnabled()) { // ⚠️DO NOT DELETE: ca sa nu chem jsonify degeaba. NICIODATA,decat daca chemi vreo functie in argumente
//      log.trace("Loan app: {}", jsonify(loanApplication));
//    }
    meterRegistry.counter("pasageriCarati").increment(2.5);
    return dto;
  }

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final List<Long> recentLoanStatusQueried = new ArrayList<>();

//@Transactional // Doamne fereste! pt ca ma blochez in synchronized calare pe o tx
  public  Status getLoanStatus(Long loanId) {
    synchronized(this) {
      LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId);
      recentLoanStatusQueried.remove(loanId); // BUG#7235 - avoid duplicates in list
      recentLoanStatusQueried.add(loanId);
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
      return loanApplication.getCurrentStatus();
    }
  }

  public List<Long> getRecentLoanStatusQueried() {
    return new ArrayList<>(recentLoanStatusQueried);
  }

  //<editor-fold desc="insert initial loans in database">
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
