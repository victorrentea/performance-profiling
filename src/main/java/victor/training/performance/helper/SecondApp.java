package victor.training.performance.helper;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.performance.profiling.entity.Audit;
import victor.training.performance.profiling.repo.AuditRepo;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static java.lang.String.join;
import static java.lang.Thread.sleep;
import static java.util.Collections.list;
import static java.util.stream.Collectors.joining;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
@RestController
public class SecondApp {
  private final JurisdictionRepo jurisdictionRepo;

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

  @GetMapping("jurisdiction")
  public String getJurisdiction() throws InterruptedException {
    List<Jurisdiction> list = jurisdictionRepo.findAllByUsername("jdoe");
    log.info("/jurisdiction takes 20 millis returning " + list);
    sleep(20);
    return "USER";
  }

  @Slf4j
  @Component // use to debugging incoming headers
  @Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1000) // run in before Spring's Security Filter Chain
  public static class PrintRequestHeadersFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
      var headersMap = getHeadersString(list(request.getHeaderNames()), name -> list(request.getHeaders(name)));
      log.info("\nRequest Headers for {}\n{}", request.getRequestURI(), headersMap);
      chain.doFilter(request, response);
    }

    private static String getHeadersString(Collection<String> names, Function<String, Collection<String>> valueByName) {
      return names.stream()
          .sorted()
          .map(name -> "\t" + name + ": " + join("; ", valueByName.apply(name)))
          .collect(joining("\n"));
    }

  }

  public static void main(String[] args) {
    // run this with VM Options:  -javaagent:/Users/victorrentea/Downloads/docker-otel-lgtm-main/examples/java/opentelemetry-javaagent-v2.1.0.jar -Dotel.instrumentation.micrometer.enabled=true -Dotel.metric.export.interval=500 -Dspring.application.name=second-app
    System.setProperty("server.port", "9999");
    SpringApplication.run(SecondApp.class, args);
  }
}
//pretend
@Entity
class Jurisdiction {
  @Id
  Long id;
  String username;
  String name;
}
interface JurisdictionRepo extends JpaRepository<Jurisdiction, Long> {
  List<Jurisdiction> findAllByUsername(String username);
}