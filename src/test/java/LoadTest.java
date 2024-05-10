import base.GatlingEngine;
import io.gatling.http.client.util.Assertions;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.time.Duration.ofSeconds;

public class LoadTest extends Simulation {
  public static void main(String[] args) {
    GatlingEngine.startClass(LoadTest.class);
  }

  {
    setUp(scenario(getClass().getSimpleName())
        .exec(http("").get("/loan/1"))
        // 23 threads will fire requests in a loop
        .injectClosed(constantConcurrentUsers(23)
            .during(ofSeconds(8))))
        .protocols(http.baseUrl("http://localhost:8080"))
        // all requests should be successful
        .assertions(global().successfulRequests().percent().gt(99.0));
  }
}
