package victor.training.performance.profiling;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.concurrent.Executors;

@Slf4j
@SpringBootApplication
@EnableFeignClients
@ImportAutoConfiguration({FeignAutoConfiguration.class})

public class ProfiledApp implements WebMvcConfigurer {
  private static final long t0 = System.currentTimeMillis();

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

  @EventListener(ApplicationReadyEvent.class)
  public void onStart() {
    log.info("🌟🌟🌟🌟🌟🌟 PerformanceApp Started in {} seconds 🌟🌟🌟🌟🌟🌟",
            (System.currentTimeMillis() - t0) / 1000);
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/", "/loan/1");
  }

  public static void main(String[] args) throws IOException {
//    ConfigureToxiproxy.delayTrafficToPostgres();
    SpringApplication.run(ProfiledApp.class, args);
  }

    // tell Tomcat to create a new virtual thread for every incoming request, unlimited number.
    // Since virtual threads are extremely LIGHT, they need not be pooled
//  @Bean
//  public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
//    return protocol -> protocol.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
//  }
}
