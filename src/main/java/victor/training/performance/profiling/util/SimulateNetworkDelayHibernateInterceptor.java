package victor.training.performance.profiling.util;

import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class SimulateNetworkDelayHibernateInterceptor extends EmptyInterceptor {

  private static final Logger log = LoggerFactory.getLogger(SimulateNetworkDelayHibernateInterceptor.class);
  public static int MILLIS = 3;

  @EventListener(ApplicationStartedEvent.class)
  public void setNetworkDelay() {
    log.info("Adding {}}ms delay/sql, to simulate real life", MILLIS);
  }

  @Override
  public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
    if (MILLIS != 0)
      PerformanceUtil.sleepMillis(MILLIS);
    return false;
  }
}