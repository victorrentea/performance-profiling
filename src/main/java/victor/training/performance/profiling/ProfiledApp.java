package victor.training.performance.profiling;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

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

  @Bean // instrumented by Micrometer and Sleuth
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean // enables the use of @Timed
  public TimedAspect timedAspect(MeterRegistry meterRegistry) {
    return new TimedAspect(meterRegistry);
  }

  @Bean
  MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    return registry -> registry.config().commonTags("application", "APP");
  }

  @Order
  @EventListener(ApplicationReadyEvent.class)
  public void onStart(ApplicationReadyEvent event) throws SQLException {
    log.info("🌟🌟🌟 App started in {}s on http://localhost:{} with DB {} 🌟🌟🌟",
        (currentTimeMillis() - t0) / 1000,
        event.getApplicationContext().getEnvironment().getProperty("local.server.port"),
        event.getApplicationContext().getBean(DataSource.class).getConnection().getMetaData().getURL());
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/", "/loan/1");
  }
}
