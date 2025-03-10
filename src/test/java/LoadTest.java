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
//        .injectOpen(// you set # requests / second
        .injectClosed(constantConcurrentUsers(23) // closed world utopia = there are a fixed number of concurrent request = 23
            .during(ofSeconds(8))))
        // ramp-test
        .protocols(http.baseUrl("http://localhost:8080"))
        // all requests should be successful
        .assertions(global().successfulRequests().percent().gt(99.0));
  }
}

// Open-world model: # requests / second
// Question: with 23 requests / second can I have more than 23 concurrent requests? Yes-how?
// For example, if my request take ~10 sec/req => I can have 230 concurrent requests