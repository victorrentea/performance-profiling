package base;

import io.gatling.app.Gatling;
import io.gatling.core.config.GatlingPropertiesBuilder;
import org.awaitility.Awaitility;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

public class GatlingEngine {
  public static void main(String[] args) {
    GatlingPropertiesBuilder props = new GatlingPropertiesBuilder()
        .resourcesDirectory(mavenResourcesDirectory().toString())
        .resultsDirectory(resultsDirectory().toString())
        .binariesDirectory(mavenBinariesDirectory().toString());
    Gatling.fromMap(props.build());

  }

  public static void startClass(Class<?> clazz) {
    waitForApp();
    clearGlowrootData();

    GatlingPropertiesBuilder props = new GatlingPropertiesBuilder()
        .resourcesDirectory(mavenResourcesDirectory().toString())
        .resultsDirectory(resultsDirectory().toString())
        .binariesDirectory(mavenBinariesDirectory().toString())
        .simulationClass(clazz.getCanonicalName());

    Gatling.fromMap(props.build());

    System.out.println("You can access Glowroot at http://localhost:4000");
    System.out.println("Flamegraph🔥🔥🔥 at http://localhost:4000/transaction/thread-flame-graph?transaction-type=Web 🔥🔥🔥");
  }

  private static void waitForApp() {
    System.out.print("Wait for app to become available ");
    Awaitility.await()
        .pollDelay(ofSeconds(1))
        .timeout(ofSeconds(10))
        .pollInterval(ofMillis(50))
        .untilAsserted(GatlingEngine::springBootActuatorUP);
    System.out.println(" UP🎉");
  }

  private static void springBootActuatorUP() {
    try {
      System.out.printf(".");
      RestTemplate restTemplate = new RestTemplate();
      Map<String, Object> responseMap = restTemplate.getForObject("http://localhost:8080/actuator/health", Map.class);
      if (!responseMap.get("status").equals("UP")) {
        throw new AssertionError("Not started yet: " + responseMap);
      }
    } catch (RestClientException e) {
      throw new AssertionError(e);
    }
  }

  private static void clearGlowrootData() {
    try {
      URI uri = URI.create("http://localhost:4000/backend/admin/delete-all-stored-data");
      HttpRequest postRequest = HttpRequest.newBuilder().POST(BodyPublishers.ofString("{}")).uri(uri).build();
      HttpClient.newHttpClient().send(postRequest, BodyHandlers.discarding());
      System.out.println("Glowroot found at localhost:4000 -> cleared its data");
    } catch (IOException | InterruptedException e) {
      System.out.println("WARN: Could not clear Glowroot data. not started on :4000?");
    }
  }


  public static Path projectRootDir() {
    try {
      return Paths.get(GatlingEngine.class.getClassLoader().getResource("gatling.conf").toURI())
          .getParent().getParent().getParent();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static Path mavenTargetDirectory() {
    return projectRootDir().resolve("target");
  }

  public static Path mavenSrcTestDirectory() {
    return projectRootDir().resolve("src").resolve("test");
  }


  public static Path mavenSourcesDirectory() {
    return mavenSrcTestDirectory().resolve("java");
  }

  public static Path mavenResourcesDirectory() {
    return mavenSrcTestDirectory().resolve("resources");
  }

  public static Path mavenBinariesDirectory() {
    return mavenTargetDirectory().resolve("test-classes");
  }

  public static Path resultsDirectory() {
    return mavenTargetDirectory().resolve("gatling");
  }

  public static Path recorderConfigFile() {
    return mavenResourcesDirectory().resolve("recorder.conf");
  }
}
