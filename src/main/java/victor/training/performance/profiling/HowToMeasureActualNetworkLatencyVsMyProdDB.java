package victor.training.performance.profiling;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.performance.profiling.repo.LoanApplicationRepo;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;

@RestController
@RequiredArgsConstructor
public class HowToMeasureActualNetworkLatencyVsMyProdDB {
  private final LoanApplicationRepo loanApplicationRepo;
  @GetMapping("measure-db-latency")
  public String measureDbLatency() {
    for (int i = 0; i < 100; i++) {
      loanApplicationRepo.findById(1L);
    }
    long t0 = nanoTime();
    loanApplicationRepo.findById(1L);
    long t1 = nanoTime();
    return "DB Latency: " + (t1 - t0) + "ns";
  }
}
