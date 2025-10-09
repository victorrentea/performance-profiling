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
        // 1) Closed World: N parallel threads firing requests in a loop
        // = # of requests in progress at any time = actual load => easier to reason
        .injectClosed(constantConcurrentUsers(23).during(ofSeconds(8))))

        // 2) Open World: N requests / second => closer to a www userbase
        // the longer the latency => the higher the concurrent load
//        .injectOpen(constantUsersPerSec(200f).during(ofSeconds(8))))
        // To convert ~= closedworld.threads/endpoint_latency = 23/100ms = 230rps (eg)

        .protocols(http.baseUrl("http://localhost:8080"))

        // ⚠️ Check all requests were successful! TRAP: Error requests tend to be very fast.
        .assertions(global().successfulRequests().percent().gt(99.0));
  }

  // ⚠️ Reproducible = Prove that rerunning the load test produces similar results!

  // ** Spike test ** = push as much load to take your system down.
  // Question: did I loose any data after I responded 200 OK to client?
  // Example #1: the app accepts a request and enqueues it in memory for later
  // Example #2: listener acks 1..1000(a page) messages to broker before processing it (which crashes)

}
