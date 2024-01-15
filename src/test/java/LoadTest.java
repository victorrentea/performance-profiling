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
    String host = "http://localhost:8080";

    setUp(scenario(getClass().getSimpleName())
        .exec(http("").get("/loan/1"))
        .injectClosed(constantConcurrentUsers(23).during(ofSeconds(15))))
        .protocols(http.baseUrl(host))
        .assertions(global().successfulRequests().percent().gt(99.0));
  }
}
