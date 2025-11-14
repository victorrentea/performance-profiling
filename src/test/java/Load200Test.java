import base.GatlingEngine;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.time.Duration.ofSeconds;

// Gatling
public class Load200Test extends Simulation {
  public static void main(String[] args) {
    GatlingEngine.startClass(Load200Test.class);
  }


  {
    setUp(scenario(getClass().getSimpleName())
        .exec(http("").get("/leak3"))
        .injectClosed(constantConcurrentUsers(20).during(ofSeconds(5))))
        .protocols(http.baseUrl("http://localhost:8080"))
        .assertions(global().successfulRequests().percent().gt(99.0));
  }

  // ⚠️ Reproducible = Prove that rerunning the load test produces similar results!

  // ** Spike test ** = push as much load to take your system down.
  // Question: did I loose any data after I responded 200 OK to client?
  // Example #1: the app accepts a request and enqueues it in memory for later
  // Example #2: listener acks 1..1000(a page) messages to broker before processing it (which crashes)

}
