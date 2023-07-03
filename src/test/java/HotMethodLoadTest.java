import base.GatlingEngine;
import io.gatling.javaapi.core.Simulation;

import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

public class HotMethodLoadTest extends Simulation {
  public static void main(String[] args) {
    GatlingEngine.startClass(HotMethodLoadTest.class);
  }

  {
    String host = "http://localhost:8080";

    setUp(scenario(getClass().getSimpleName())
            .exec(http("less data (29.999)")
                    .post("/payments/delta")
                    .header("Content-Type", "application/json")
                    .body(StringBody(generateData(29_999))))
            .exec(http("more data (32.000)")
                    .post("/payments/delta")
                    .header("Content-Type","application/json")
                    .body(StringBody(generateData(32_000))))
            .injectClosed(constantConcurrentUsers(8).during(ofSeconds(8))))

            .protocols(http.baseUrl(host));
  }

  private static String generateData(int size) {
    List<Long> lessData = LongStream.rangeClosed(1, size).boxed().collect(toList());
    Collections.shuffle(lessData);
    String bodyStr = lessData.toString();
    return bodyStr;
  }
}
