import base.GatlingEngine;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

public class LoadFloorsTest extends Simulation {
  {
    String host = "http://localhost:8080";

    setUp(scenario(getClass().getSimpleName()+"-5").exec(http("")
                .get("/loan/1"))
            .injectClosed(
                constantConcurrentUsers(5).during(5)
            ),
        scenario(getClass().getSimpleName()+"-10").exec(http("")
                .get("/loan/1"))
            .injectClosed(
                constantConcurrentUsers(10).during(5)
            ),
        scenario(getClass().getSimpleName()+"-15").exec(http("")
                .get("/loan/1"))
            .injectClosed(
                constantConcurrentUsers(15).during(5)
            ))

        .protocols(http.baseUrl(host));
  }

  public static void main(String[] args) {
    GatlingEngine.startClass(LoadFloorsTest.class);
  }
}
