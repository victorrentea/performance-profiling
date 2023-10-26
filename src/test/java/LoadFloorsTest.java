import base.GatlingEngine;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public class LoadFloorsTest extends Simulation {
  {
    String host = "http://localhost:8080";

    setUp(scenario(getClass().getSimpleName()).exec(http("")
            .get("/loan/1"))
        .injectClosed(
            constantConcurrentUsers(5).during(5),
            constantConcurrentUsers(10).during(5),
            constantConcurrentUsers(15).during(5),
            constantConcurrentUsers(20).during(5),
            constantConcurrentUsers(25).during(5),
            constantConcurrentUsers(30).during(5)
          ))

        .protocols(http.baseUrl(host));
  }

  public static void main(String[] args) {
    GatlingEngine.startClass(LoadFloorsTest.class);
  }
}
