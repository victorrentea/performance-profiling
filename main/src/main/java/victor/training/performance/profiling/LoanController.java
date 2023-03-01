package victor.training.performance.profiling;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import victor.training.performance.profiling.LoanApplication.Status;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LoanController {
   private final LoanService loanService;

   @GetMapping("loan/{id}")
   public LoanApplicationDto get(@PathVariable Long id) {
      return loanService.getLoanApplication(id);
   }

   @GetMapping("loan/{id}/status")
   public Status getStatus(@PathVariable Long id) {
      return loanService.getLoanApplicationStatusForClient(id);
   }

   @GetMapping("loan/recent-queried")
   public List<Long> getLoanApplicationStatus() {
      return loanService.getRecentLoanStatusQueried();
   }

}

