package victor.training.performance.profiling.util;

import org.hibernate.EmptyInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SimulateNetworkDelayHibernateInterceptor extends EmptyInterceptor {

  private static final Logger log = LoggerFactory.getLogger(SimulateNetworkDelayHibernateInterceptor.class);
  public static int MILLIS = 3;// NETWORK OVERHEAD DUS-INTORS pana la PG

  @EventListener(ApplicationStartedEvent.class)
  public void setNetworkDelay() {
    log.info("Adding {}}ms delay/sql, to simulate real life", MILLIS);
  }

  @Override
  public String onPrepareStatement(String sql) {
    if (MILLIS != 0)
      PerformanceUtil.sleepMillis(MILLIS);
    return sql;
  }
}