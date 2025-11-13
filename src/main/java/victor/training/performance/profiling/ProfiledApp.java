package victor.training.performance.profiling;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskDecorator;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import static java.lang.System.currentTimeMillis;

@Slf4j
@SpringBootApplication
@EnableFeignClients
@ImportAutoConfiguration({FeignAutoConfiguration.class})
public class ProfiledApp implements WebMvcConfigurer {
  private static final long t0 = System.currentTimeMillis();

  public static void main(String[] args) throws IOException {
    SpringApplication.run(ProfiledApp.class, args);
  }

  @Bean
  public ThreadPoolTaskExecutor executor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);//always on workers
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(5); // rejected tasks => 503 = primitive form of backpressure
    executor.setRejectedExecutionHandler(new CallerRunsPolicy());
    // dangerous: can starve the tomcat's thread pool => app freeze
    //  => k8s liveness every 5s -> /health times out 5x => kill pod =DOS
    //  => critical endpoints /place-order might 503/timeout

    executor.setThreadNamePrefix("my-");
    return executor;
  }

  @Bean // instrumented by micrometer-tracing
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
  }

  @Bean // enables the use of @Timed
  public TimedAspect timedAspect(MeterRegistry registry) {
    return new TimedAspect(registry);
  }

  @Bean // enables the use of @Observed
  public ObservedAspect observedAspect(ObservationRegistry registry) {
    return new ObservedAspect(registry);
  }

  @Bean
  MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    return registry -> registry.config().commonTags("application", "APP");
  }



  @Bean // propagate tracing over all Spring-managed thread pools <<< copy this to your app
  public TaskDecorator taskDecorator() {
    return (runnable) -> ContextSnapshot.captureAll().wrap(runnable);
  }

  @Order
  @EventListener
  public void onStart(ApplicationStartedEvent event) throws SQLException {
    log.info("🌟🌟🌟 App started in {}s on http://localhost:{} with DB {} : view at http://localhost:{}/h2-console (user:sa, pass:sa) 🌟🌟🌟",
        (currentTimeMillis() - t0) / 1000,
        event.getApplicationContext().getEnvironment().getProperty("local.server.port"),
        event.getApplicationContext().getBean(DataSource.class).getConnection().getMetaData().getURL(),
        event.getApplicationContext().getEnvironment().getProperty("local.server.port"));
    checkGlowroot();
  }

  private static void checkGlowroot() {
    try {
      new RestTemplate().getForEntity("http://localhost:4000", String.class);
      log.info("✅ Glowroot available at http://localhost:4000");
    } catch (Exception e) {
      log.warn("❌ Glowroot not available. To enable, download it from https://glowroot.org/ and add -javaagent:/path/to/glowroot.jar to your VM options");
    }
  }

  @Override // redirect from / to /loan/1
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/", "/loan/1");
  }
}
