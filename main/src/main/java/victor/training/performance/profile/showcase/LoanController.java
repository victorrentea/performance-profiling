package victor.training.performance.profile.showcase;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import victor.training.performance.profile.showcase.LoanApplication.Status;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("profile/showcase")
@RequiredArgsConstructor
public class LoanController {
   private final LoanService loanService;

   @GetMapping("{id}")
   public LoanApplicationDto get(@PathVariable Long id) {
      return loanService.getLoanApplication(id);
   }

   @GetMapping("{id}/status")
   public Status getStatus(@PathVariable Long id) {
      return loanService.getLoanApplicationStatusForClient(id);
   }

   @GetMapping("recent-queried")
   public List<Long> getLoanApplicationStatus() {
      return loanService.getRecentLoanStatusQueried();
   }

}

