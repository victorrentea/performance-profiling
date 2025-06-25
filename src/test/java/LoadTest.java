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

  // spike test = push as much load to take your system down.
  // Question: did I loose any data after a 200 to client?
  // Example #1: the app accepts a request and enqueues it in
  // Example #2: listener acks 1..1000(page) message to broker before processing it, then processing crashes

  {
    setUp(scenario(getClass().getSimpleName())
        .exec(http("").get("/loan/1"))
        // 1) Closed World: 23 parallel requests at all times = real issue = easier to reason
        .injectClosed(constantConcurrentUsers(23).during(ofSeconds(8))))

        // 2) Open World: 200 requests / second = closer to real world;
//        .injectOpen(constantUsersPerSec(200f).during(ofSeconds(8))))
        // To convert ~= closedworld.threads/endpoint_latency = 23/100ms = 230rps (eg)

        .protocols(http.baseUrl("http://localhost:8080"))
        // ⚠️ Fast-Errors: Check all requests were successful!
        .assertions(global().successfulRequests().percent().gt(99.0));
  }

  // ⚠️ Reproducible: Prove that rerunning the load test produces similar results!
}
