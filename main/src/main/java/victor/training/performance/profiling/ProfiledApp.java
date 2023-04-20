package victor.training.performance.profiling;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@SpringBootApplication
@EnableFeignClients
public class ProfiledApp implements WebMvcConfigurer {
  private static final long t0 = System.currentTimeMillis();

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean // enables the use of @Timed
  public TimedAspect timedAspect(MeterRegistry meterRegistry) {
    return new TimedAspect(meterRegistry);
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onStart() {
    log.info("🌟🌟🌟🌟🌟🌟 PerformanceApp Started in {} seconds 🌟🌟🌟🌟🌟🌟",
            (System.currentTimeMillis() - t0) / 1000);
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/", "/loan/1");
  }

  public static void main(String[] args) {
    SpringApplication.run(ProfiledApp.class, args);
  }
}