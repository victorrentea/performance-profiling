package victor.training.performance.profiling;

import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
//@Transactional // @TransactionAttribute(EJB)
public class LoanService {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;

  public LoanApplicationDto getLoanApplication(Long loanId) {
    List<CommentDto> comments = commentsApiClient.fetchComments(loanId); // = 66%
    LoanApplication loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId); // = 28%
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);

    log.trace("Loan app: {}", loanApplication); // #asada ca nu eval toStringu lu loan

//    log.trace("Loan app: " + loanApplication); // PROST: fac un string (mare) pe care il arunc 5% -> SELECT !!! in 🤬

//    if (log.isTraceEnabled()) {
//      log.trace("Loan app: {}", jsonify(loanApplication)); // 5% -> SELECT !!! in 🤬
//    }

    // atTrace nici nu executa lambda data lu "log()" daca nu e level pe trace; FP-mania
//    log.atTrace().log(()->"Loan app: "+ jsonify(loanApplication)); // 5% -> SELECT !!! in 🤬

    return dto;
  }

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }



  public  Status getLoanStatus(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findById(loanId).orElseThrow();
    boundedList.add(loanId);
    return loanApplication.getCurrentStatus();
//    return loanApplicationRepo.findStatusById(loanId); // #like
  }

  private final BoundedList boundedList = new BoundedList(10);

  private final ThreadPoolTaskExecutor executor;

  public List<Long> getRecentLoanStatusQueried() {
    log.info("In parent thread");
    CompletableFuture.runAsync(() -> log.info("In a child thread"), executor).join();
    return boundedList.getCopy();
  }

}
