import base.GatlingEngine;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.time.Duration.ofSeconds;

public class LockContentionLoadTest extends Simulation {
  public static void main(String[] args) {
    GatlingEngine.startClass(LockContentionLoadTest.class);
  }

  {
    String host = "http://localhost:8080";

    setUp(scenario(getClass().getSimpleName()).exec(http("")
                    .get("/loan/1/status"))
            .injectClosed(constantConcurrentUsers(10).during(ofSeconds(8))))

            .protocols(http.baseUrl(host));
  }
}
