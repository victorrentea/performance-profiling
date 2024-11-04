package victor.training.performance.profiling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import victor.training.performance.profiling.dto.LoanApplicationDto;
import victor.training.performance.profiling.entity.LoanApplication;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LoanController {
  private final LoanService loanService;

  // @GET @Path("loan/{id}") // <<<<
  @GetMapping("loan/{id}") // <<<<
  public LoanApplicationDto get(@PathVariable Long id) {
    return loanService.getLoanApplication(id);
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

