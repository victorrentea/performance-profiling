package victor.training.performance.profiling;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
import victor.training.performance.profiling.util.ConfigureToxiproxy;

import java.io.IOException;
import java.util.concurrent.Executors;

@Slf4j
@SpringBootApplication
@EnableFeignClients
@ImportAutoConfiguration({FeignAutoConfiguration.class})

public class ProfiledApp implements WebMvcConfigurer {
  private static final long t0 = System.currentTimeMillis();


  @Bean
  public MeterFilter fi() {
    return new MeterFilter() {
      @Override
      public MeterFilterReply accept(Meter.Id id) {
        return MeterFilterReply.ACCEPT;
      }

      @Override
      public Meter.Id map(Meter.Id id) {
        String v = MDC.get("clientId");

        Tag tag = Tag.of("clientId", v!=null ? v : "no-client-id");
        return id.withTag(tag);
      }

      @Override
      public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        return MeterFilter.super.configure(id, config);
      }
    };
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
//    return registry -> registry.config().commonTags("application", "APP");

      return registry -> registry.config().meterFilter(fi());

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
    SpringApplication.run(ProfiledApp.class, args);
  }

    // tell Tomcat to create a new virtual thread for every incoming request, unlimited number.
    // Since virtual threads are extremely LIGHT, they need not be pooled
//  @Bean
//  public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
//    return protocol -> protocol.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
//  }
}
