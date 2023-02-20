import base.GatlingEngine;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.time.Duration.ofSeconds;

public class Showcase extends Simulation {
  public static void main(String[] args) {
    GatlingEngine.startClass(Showcase.class);
  }

  {
    String host = "http://localhost:8080";

    setUp(scenario(getClass().getSimpleName()).exec(http("")
                    .get("/profile/showcase/1"))
            .injectClosed(constantConcurrentUsers(23).during(ofSeconds(8))))

            .protocols(http.baseUrl(host));
  }
}
