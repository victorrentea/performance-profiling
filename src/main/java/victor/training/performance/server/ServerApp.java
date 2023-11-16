package victor.training.performance.server;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SpringBootApplication
@RestController
public class ServerApp {
  public static void main(String[] args) {
    new SpringApplicationBuilder(ServerApp.class)
        .properties(Map.of("server.port", "8081"))
        .run(args);
//    SpringApplication.run(ServerApp.class, args);
  }

  @GetMapping("data")
  public String get() {
    return "data";
  }
}
