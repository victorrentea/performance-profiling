package victor.training.performance.profiling;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import victor.training.performance.profiling.dto.LoanApplicationDto;
import victor.training.performance.profiling.entity.LoanApplication;

import java.time.Duration;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LoanController {
  private final LoanService loanService;
  private final MeterRegistry meterRegistry;

  @GetMapping("loan/{id}")
  public LoanApplicationDto get(@PathVariable Long id) {
    return loanService.getLoanApplication(id);
  }

  @PostMapping("loan/{title}")
  public void save(@PathVariable String title) {
    meterRegistry.counter("loans_created").increment();
    loanService.saveLoanApplication(title);
  }

  @GetMapping("loan/{id}/status")
  public LoanApplication.Status getStatus(@PathVariable Long id) {
    Timer timer = Timer.builder("get_loan_status")
        .publishPercentiles(0.5, 0.9, 0.99) // collect percentiles like median, p90, p99
        .publishPercentileHistogram(true)
        .sla(Duration.ofMillis(100), Duration.ofMillis(500), Duration.ofMillis(1000)) // SLA Buckets
        .register(meterRegistry);
    return timer.record(() -> loanService.getLoanStatus(id));
  }

  @GetMapping("loan/recent")
  public List<Long> getLoanApplicationStatus() {
    return loanService.getRecentLoanStatusQueried();
  }


  @GetMapping("txflush")
  public void txFlush() {
    loanService.autoFlushFaraTx();
  }
}

