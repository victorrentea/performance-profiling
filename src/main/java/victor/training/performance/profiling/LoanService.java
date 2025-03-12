package victor.training.performance.profiling;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import victor.training.performance.helper.Sleep;
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
public class LoanService /*extends NeverDoThis*/ {
  private final LoanApplicationRepo loanApplicationRepo;
  private final CommentsApiClient commentsApiClient;
  private final MeterRegistry meterRegistry;
  private final ThreadPoolTaskExecutor myExecutor;

  @SneakyThrows
  public CompletableFuture<LoanApplicationDto> getLoanApplication(Long loanId) {
    log.info("LoanX: {}", loanId); // Fix#1

    CompletableFuture<List<CommentDto>> futureCommentsFromAPI = getFetchComments(loanId); // nonblocking (pretend)

    CompletableFuture<LoanApplication> futureLoanFromDB = findLoan(loanId);

//    auditToInsert

    // webFlux equivalent would be
    // monoA.zipWith(monoB, (a,b) -> new LoanApplicationDto(a,b))
    return futureLoanFromDB.thenCombine(
        futureCommentsFromAPI,
        (loan, comments) -> new LoanApplicationDto(loan, comments));
  }

  RestTemplate e;

  private CompletableFuture<LoanApplication> findLoan(Long loanId) {
    return CompletableFuture.supplyAsync(() -> {
      Sleep.millis(1000); //add brutal sleep to prove concurrency benefit
      return loanApplicationRepo.findByIdLoadingSteps(loanId);
    }, myExecutor);
  }

  private CompletableFuture<List<CommentDto>> getFetchComments(Long loanId) {
    log.info("IN fetch comments");
    // non-blocking API call: gives you a CompletableFuture as a result of the call
    // but it does not BLOCK your thread for any ms.
    // reactive programming with spring-webflux
//    CompletableFuture<List<CommentDto>> futureComment = webClient.get()
//        .uri(baseUrl+"loan-comments/{id}",loanId)
//        .retrieve()
//        .entityToMono(CommentDto.class)
//        .toFuture(); // A) break out of Reactive Stream world back into COmpletableFuture ("promise-style")
//        .block(); // B) hang this thread until the result is available
    return CompletableFuture.supplyAsync(() -> {
      log.info("In what thread am I ?");
      Sleep.millis(1000); //add brutal sleep to prove concurrency benefit
      return commentsApiClient.fetchComments(loanId);
    }, myExecutor);
  }
// parallel on Mar 12 at 3:34 I got 47 ms
// sequential on Mar 12 at 3:34 I got 54 ms

  private final AuditRepo auditRepo;

  @Transactional
  public void saveLoanApplication(String title) {
    Long id = loanApplicationRepo.save(new LoanApplication().setTitle(title)).getId();
    auditRepo.save(new Audit("Loan created: " + id));
  }

  private final LastRecentlyUsed lastRecentlyUsed = new LastRecentlyUsed();

  //  @Transactional // crime to combine with synchronized. not even needed here, as i only SELECT
  public Status getLoanStatus(Long loanId) {
    LoanApplication loanApplication = loanApplicationRepo.findById(loanId).orElseThrow();
    lastRecentlyUsed.updateLastUsed(loanId);
    return loanApplication.getCurrentStatus(); // 5%
  }

  private final ThreadPoolTaskExecutor executor;

  public List<Long> getRecentLoanStatusQueried() {
    log.info("In parent thread");
    CompletableFuture.runAsync(() -> log.info("In a child thread"), executor).join();
    return lastRecentlyUsed.get();
  }

}
