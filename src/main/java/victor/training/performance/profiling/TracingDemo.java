package victor.training.performance.profiling;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
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

    Baggage baggage = Baggage.current() // sent as 'baggage' request header to other services
        .toBuilder()
        .put("loan.id", String.valueOf(loanId))
        .put("user.id", "jdoe")
        .build();

    var commentsFuture = CompletableFuture.supplyAsync(() -> {
      var spanComments = GlobalOpenTelemetry.getTracer("profiling.app")
          .spanBuilder("fetch.comments")
          .setAttribute("loan.id", loanId)
          .startSpan();
      // Propagate baggage in the async call
      try (Scope ignored = baggage.makeCurrent();
           Scope ignored2 = spanComments.makeCurrent()) {
        log.info("Before fetch");
        return commentsApiClient.fetchComments(loanId);
      } finally {
        spanComments.end();
      }
    }, executor);

    var spanFind = GlobalOpenTelemetry.getTracer("profiling.app")
        .spanBuilder("fetch.loanApplication")
        .startSpan();
    Loan loanApplication;
    try (Scope ignored = spanFind.makeCurrent()) {
      log.info("Before loan fetch");
      loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId);
    } finally {
      spanFind.end();
    }

    // fire-and-forget
    var backgroundSpan = GlobalOpenTelemetry.getTracer("profiling.app")
        .spanBuilder("background.work")
        .startSpan();

    CompletableFuture.runAsync(() -> {
      try (Scope ignored = backgroundSpan.makeCurrent()) {
        log.info("Start background work");
        PerformanceUtil.sleepMillis(100);
        log.info("After background work");
      } finally {
        backgroundSpan.end();
      }
    });

    List<CommentDto> comments = commentsFuture.join();
    var dto = new LoanDto(loanApplication, comments);
    log.trace("End");
    return dto;
  }
}
