package victor.training.performance.profile.showcase;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
      meterRegistry.counter("statusfee").increment(0.01);
      return loanService.getLoanApplicationStatusForClient(id);
   }

   @GetMapping("recent-queried")
   public List<Long> getLoanApplicationStatus() {
      return loanService.getRecentLoanStatusQueried();
   }


   @Autowired
   private MeterRegistry meterRegistry;
}

