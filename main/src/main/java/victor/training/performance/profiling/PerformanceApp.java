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

@Slf4j
@SpringBootApplication
@EnableFeignClients
public class PerformanceApp {
    private static final long t0 = System.currentTimeMillis();

    @Bean
    public RestTemplate rest() {
        return new RestTemplate();
        // it is critical to define RestTemplate as a @Bean rather than instantiating it at usage point,
        // to allow Apache Sleuth to hack it to send its TraceID over HTTP request headers
    }

    @Bean // enables the use of @Timed on methods
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }

    @EventListener
    public void onStart(ApplicationReadyEvent event) {
        long t1 = System.currentTimeMillis();

        log.info("🌟🌟🌟🌟🌟🌟 PerformanceApp Started in {} seconds 🌟🌟🌟🌟🌟🌟", (t1-t0)/1000);
    }

    public static void main(String[] args) {
        SpringApplication.run(PerformanceApp.class, args);
    }
}
