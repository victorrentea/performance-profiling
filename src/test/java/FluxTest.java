import base.GatlingEngine;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.time.Duration.ofSeconds;

public class FluxTest extends Simulation {
  public static void main(String[] args) {
    GatlingEngine.startClass(FluxTest.class);
  }

  {
    String host = "http://localhost:8080";

    setUp(scenario(getClass().getSimpleName()).exec(http("")
                    .get("/flux"))
                   .injectClosed(rampConcurrentUsers(0).to(2000).during(5), // grow over 5 sec
                         constantConcurrentUsers(2000).during(10))) // 1
                 // 2))

            .protocols(http.baseUrl(host));
  }
}
