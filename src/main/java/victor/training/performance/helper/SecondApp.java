package victor.training.performance.helper;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.performance.profiling.util.PrintRequestHeadersFilter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.Thread.sleep;
import static java.util.concurrent.CompletableFuture.delayedExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
@RestController
@Import(PrintRequestHeadersFilter.class)
public class SecondApp {
  private final JurisdictionRepo jurisdictionRepo;

  public record CommentDto(String body) {
  }

  @GetMapping("jurisdiction")
  public CompletableFuture<String> getJurisdiction() throws InterruptedException {
    List<Jurisdiction> list = jurisdictionRepo.findAllByUsername("jdoe");
    log.info("/jurisdiction takes 20 millis to return " + list);
    return CompletableFuture.supplyAsync(() -> "USER", delayedExecutor(20, MILLISECONDS));
  }

  @GetMapping("loan-comments/{id}")
  public CompletableFuture<List<CommentDto>> getComments() throws InterruptedException {
    var result = List.of(new CommentDto("LGTM!"), new CommentDto("NACK!"));
    log.info("/loan-comments takes 10 millis");
    return CompletableFuture.supplyAsync(() -> result, delayedExecutor(10, MILLISECONDS));
  }

  @EventListener(ApplicationStartedEvent.class)
  public void printAppStarted() {
    log.info("""
           Second App Started on http://localhost:9999 
           Check at:
           http://localhost:9999/jurisdiction
           http://localhost:9999/loan-comments/1
        """);
  }

  public static void main(String[] args) {
    System.setProperty("server.port", "9999");
    SpringApplication.run(SecondApp.class, args);
  }
}

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