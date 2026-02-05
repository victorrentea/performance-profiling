package victor.training.performance.profiling;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import victor.training.performance.profiling.dto.CommentDto;
import victor.training.performance.profiling.dto.LoanApplicationDto;
import victor.training.performance.profiling.entity.LoanApplication;
import victor.training.performance.profiling.repo.LoanApplicationRepo;
import victor.training.performance.profiling.util.PerformanceUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Service
public class TracingDemo {
  private final CommentsApiClient commentsApiClient;
  private final LoanApplicationRepo loanApplicationRepo;
  private final ThreadPoolTaskExecutor executor;

  public LoanApplicationDto getLoanApplication(Long loanId) {
    log.trace("Start");
    Span.current().setAttribute("loan.id", loanId);

    var commentsFuture = CompletableFuture.supplyAsync(() -> {
      var spanComments = GlobalOpenTelemetry.getTracer("profiling.app")
          .spanBuilder("fetch.comments")
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
        .startSpan();
    LoanApplication loanApplication;
    try (Scope ignored = spanFind.makeCurrent()) {
      log.info("Before loan fetch");
      loanApplication = loanApplicationRepo.findByIdLoadingSteps(loanId);
    } finally {
      spanFind.end();
    }

    // fire-and-forget
    var span3 = GlobalOpenTelemetry.getTracer("profiling.app")
        .spanBuilder("background.work")
        .startSpan();

    CompletableFuture.runAsync(() -> {
      try (Scope ignored = span3.makeCurrent()) {
        log.info("Start background work");
        PerformanceUtil.sleepMillis(100);
        log.info("After background work");
      } finally {
        span3.end();
      }
    });

    List<CommentDto> comments = commentsFuture.join();
    LoanApplicationDto dto = new LoanApplicationDto(loanApplication, comments);
    log.trace("End");
    return dto;
  }
}
