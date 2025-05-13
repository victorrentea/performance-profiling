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
        // For searching for the breaking point:
        // on what load do I start seeing errors or exceed my SLA
        // - you can throttle (ratelimit) your own ingress/pod to stay below the breaking point
        //    a) global throttling per my instance =>503 Service unavailable
        //    b) Fairness: throttle / client id => 429 Too many request = you track client JWT/Basic [HARD]
        // - request more hardware / how many pods you might need in prod
        // ⚠️ it's idealized. no other endpoint is called at the same time.

        .protocols(http.baseUrl(host));
  }

  public static void main(String[] args) {
    GatlingEngine.startClass(LoadRampTest.class);
  }
}
