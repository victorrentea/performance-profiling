import base.GatlingEngine;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public class LoadRampTest extends Simulation {
  {
    String host = "http://localhost:8080";

    setUp(scenario(getClass().getSimpleName()).exec(http("")
            .get("/loan/1"))
        .injectClosed(rampConcurrentUsers(0).to(30).during(15)))

        .protocols(http.baseUrl(host));
  }

  public static void main(String[] args) {
    GatlingEngine.startClass(LoadRampTest.class);
  }
}
