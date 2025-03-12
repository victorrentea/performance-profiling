package victor.training.performance.profiling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import victor.training.performance.profiling.dto.LoanApplicationDto;
import victor.training.performance.profiling.entity.LoanApplication;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.System.currentTimeMillis;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LoanController {
  private final LoanService loanService;

  @GetMapping("loan/{id}")
  public CompletableFuture<LoanApplicationDto> get(@PathVariable Long id) {
    long t0 = currentTimeMillis();
    try {
      return loanService.getLoanApplication(id);
    } finally {
      long t1 = currentTimeMillis();
      log.info("HTTP Thread released in {} ms", t1 - t0);
    }
  }

  @PostMapping("loan/{title}")
  public void save(@PathVariable String title) {
    loanService.saveLoanApplication(title);
  }

  @GetMapping("loan/{id}/status")
  public LoanApplication.Status getStatus(@PathVariable Long id) {
    return loanService.getLoanStatus(id);
  }

  @GetMapping("loan/recent")
  public List<Long> getLoanApplicationStatus() {
    return loanService.getRecentLoanStatusQueried();
  }

}

