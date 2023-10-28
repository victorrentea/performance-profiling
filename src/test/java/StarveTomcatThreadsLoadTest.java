import base.GatlingEngine;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.time.Duration.ofSeconds;

public class StarveTomcatThreadsLoadTest extends Simulation {
  public static void main(String[] args) {
    GatlingEngine.startClass(StarveTomcatThreadsLoadTest.class);
  }

  {
    String host = "http://localhost:8080";

    setUp(scenario(getClass().getSimpleName() + "-200")
            .exec(http("200")
                .get("/loan/1/comments-bis"))
            .injectClosed(constantConcurrentUsers(200).during(ofSeconds(5))),
        scenario(getClass().getSimpleName() + "-250")
            .exec(http("250")
                .get("/loan/1/comments-bis"))
            .injectClosed(constantConcurrentUsers(250).during(ofSeconds(5))))

        .protocols(http.baseUrl(host));
  }
}
