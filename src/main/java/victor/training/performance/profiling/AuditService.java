package victor.training.performance.profiling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import victor.training.performance.helper.Sleep;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuditService {
  @Async("myExecutor")
  public void insertAudit() {
    Sleep.millis(1000);
    log.info("Send an audit");
    throw new IllegalArgumentException("ERRORS WILL HAPPEN - murhpy's law");
  }
}
