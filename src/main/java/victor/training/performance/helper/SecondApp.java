package victor.training.performance.helper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.performance.profiling.ProfiledApp;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.String.join;
import static java.lang.Thread.sleep;
import static java.util.Collections.list;
import static java.util.stream.Collectors.joining;

@Slf4j
@SpringBootApplication
@RestController
public class SecondApp {
  public static void main(String[] args) {
    // run this with VM Options:  -javaagent:/Users/victorrentea/Downloads/docker-otel-lgtm-main/examples/java/opentelemetry-javaagent-v2.1.0.jar -Dotel.instrumentation.micrometer.enabled=true -Dotel.metric.export.interval=500 -Dspring.application.name=second-app
    System.setProperty("server.port", "9999");
    SpringApplication.run(SecondApp.class, args);
  }


  @GetMapping("jurisdiction")
  public String getJurisdiction() throws InterruptedException {
    log.info("/jurisdiction takes 20 millis");
    sleep(20);
    return "ADMIN";
  }

  public record CommentDto(String body) {
  }

  @GetMapping("loan-comments/{id}")
  public List<CommentDto> getComments() throws InterruptedException {
    log.info("/loan-comments takes 10 millis");
    sleep(10);
    return List.of(new CommentDto("LGTM!"), new CommentDto("NACK!"));
  }

  @EventListener(ApplicationStartedEvent.class)
  public void printAppStarted() {
    log.info("""
           Second App Started on http://localhost:9999 Check at:
           http://localhost:9999/jurisdiction
           http://localhost:9999/loan-comments/1
        """);
  }

  @Slf4j
@Component // use for debugging incoming headers
  @Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1000) // run in before Spring's Security Filter Chain
  public static class HeaderPrinterFilter extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
      log.info("\nRequest Headers for " + request.getRequestURI() + "\n" + getHeadersAsMap(list(request.getHeaderNames()), name -> list(request.getHeaders(name))));
      chain.doFilter(request, response);
//      log.info("\nResponse Headers for " + request.getRequestURI() + "\n" + getHeadersAsMap(response.getHeaderNames(), response::getHeaders));
    }

    private static String getHeadersAsMap(Collection<String> names, Function<String, Collection<String>> valueByName) {
      return names.stream()
          .sorted()
          .map(name -> "\t" + name + ": " + join("; ", valueByName.apply(name)))
          .collect(joining("\n"));
    }
  }

}
