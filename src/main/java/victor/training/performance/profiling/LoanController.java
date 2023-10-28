package victor.training.performance.profiling;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import victor.training.performance.profiling.dto.CommentDto;
import victor.training.performance.profiling.dto.LoanApplicationDto;
import victor.training.performance.profiling.entity.LoanApplication;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LoanController {
  private final LoanService loanService;

  @GetMapping("loan/{id}")
  public LoanApplicationDto get(@PathVariable Long id) {
    return loanService.getLoanApplication(id);
  }
  @GetMapping("loan/{id}/comments-bis")
  public List<CommentDto> getBis(@PathVariable Long id) {
    return loanService.getLoanCommentsBis(id);
  }

  @PostMapping("loan/{title}")
  public void save(@PathVariable String title) {
    loanService.saveLoanApplication(title);
  }

  private final MeterRegistry meterRegistry;
  @GetMapping("loan/{id}/status")
  public LoanApplication.Status getStatus(@PathVariable Long id) {
//    meterRegistry.gauge()
//    meterRegistry.timer("").record(()->{....});
    return loanService.getLoanApplicationStatusForClient(id);
  }

  @GetMapping("loan/recent-queried")
  public List<Long> getLoanApplicationStatus() {
    return loanService.getRecentLoanStatusQueried();
  }

  @PostMapping("payments/delta")
  public int getUnprocessedPayments(@RequestBody List<Long> newPaymentIds) {
    return loanService.getUnprocessedPayments(newPaymentIds);
  }
}

