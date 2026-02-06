package victor.training.performance.profiling;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import victor.training.performance.profiling.dto.CommentDto;
import victor.training.performance.profiling.dto.LoanDto;
import victor.training.performance.profiling.entity.Loan;
import victor.training.performance.profiling.repo.LoanRepo;
import victor.training.performance.profiling.util.PerformanceUtil;
import victor.training.performance.profiling.util.Sleep;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Service
public class TracingDemo {
  private final CommentsApiClient commentsApiClient;
  private final LoanRepo loanApplicationRepo;
  private final ThreadPoolTaskExecutor executor;

  public LoanDto getLoanApplication(Long loanId) {
    log.trace("Start");
    Span.current().setAttribute("loan.id", loanId);

    Baggage baggage = Baggage.current()
        .toBuilder()
        .put("loan.id", String.valueOf(loanId))
        .put("user.id", "jdoe")
        .build();

    // Capture parent context for async propagation
    Context parentContext = Context.current().with(baggage);

    var commentsFuture = CompletableFuture.supplyAsync(() -> {
      var spanComments = GlobalOpenTelemetry.getTracer("profiling.app")
          .spanBuilder("fetch.comments")
          .setParent(parentContext) // Set parent context explicitly
          .setAttribute("loan.id", loanId)
          .startSpan();
      try (Scope ignored = spanComments.makeCurrent()) {
        log.info("Before fetch");
        return commentsApiClient.fetchComments(loanId);
      } finally {
        spanComments.end();
      }
    }, executor);

    var spanFind = GlobalOpenTelemetry.getTracer("profiling.app")
        .spanBuilder("fetch.loanApplication")
        .setParent(Context.current()) // Set parent context explicitly
        .startSpan();
    Loan loanApplication;
    try (Scope ignored = spanFind.makeCurrent()) {
      log.info("Before loan fetch");
      loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId);
    } finally {
      spanFind.end();
    }

    CompletableFuture.runAsync(() -> {
      var backgroundSpan = GlobalOpenTelemetry.getTracer("profiling.app")
          .spanBuilder("background.work")
          .setParent(parentContext)
          .startSpan();
      try (Scope ignored = backgroundSpan.makeCurrent()) {
        log.info("Start background work");
        Sleep.millis(100);
        log.info("After background work");
      } finally {
        backgroundSpan.end();
      }
    }, executor);

    List<CommentDto> comments = commentsFuture.join();
    var dto = new LoanDto(loanApplication, comments);
    log.trace("End");
    return dto;
  }
}
