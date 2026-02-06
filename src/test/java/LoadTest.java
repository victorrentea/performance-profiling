import base.GatlingEngine;
import io.gatling.http.client.util.Assertions;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.time.Duration.ofSeconds;

// Gatling vs K6
public class LoadTest extends Simulation {
  public static void main(String[] args) {
    GatlingEngine.startClass(LoadTest.class);
  }

  {
    setUp(scenario(getClass().getSimpleName())
        .exec(http("").get("/loan/1"))
        // 1) Closed World (easier): N parallel threads firing requests in a loop => max N concurrent requests at any time
        .injectClosed(constantConcurrentUsers(23).during(ofSeconds(8))))

        // 2) Open World: N requests / second => closer to a www userbase: higher latency => higher concurrency
        // .injectOpen(constantUsersPerSec(200f).during(ofSeconds(8))))

        // To convert ~= closedworld.threads/endpoint_latency = 23/100ms = 230rps (eg)

        .protocols(http.baseUrl("http://localhost:8080"))

        // ⚠️ Check all requests were successful! TRAP: Error requests tend to be very fast.
        .assertions(global().successfulRequests().percent().gt(99.0));
  }
  // ⚠️ Before you begin, prove that rerunning the load test produces similar results!
}
