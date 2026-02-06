package base;

import io.gatling.app.Gatling;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.CompletableFuture.*;
import static java.util.concurrent.TimeUnit.SECONDS;

public class GatlingEngine {
  public static void main(String[] args) {
    String[] gatlingArgs = new String[] {
        "-rf", resultsDirectory().toString()
    };
    runGatlingInFork(gatlingArgs);
  }

  public static void startClass(Class<?> clazz) {
    waitForApp();

//    clearGlowrootData();
    // clear JFR after Gatling starts to give time to JVM to warmup
    runAsync(GatlingEngine::clearGlowrootData, delayedExecutor(3, SECONDS));

    String[] gatlingArgs = new String[] {
        "-s", clazz.getCanonicalName(),
        "-rf", resultsDirectory().toString()
    };
    int code = runGatlingInFork(gatlingArgs);
    if (code != 0) {
      System.err.println("❌❌❌ Some Requests were in ERROR (exit code=" + code + ") ❌❌❌");
    }

    // TODO can I get mean programatically printed here?
    System.out.println("Glowroot  : http://localhost:4000");
    System.out.println("Flamegraph: http://localhost:4000/transaction/thread-flame-graph?transaction-type=Web 🔥🔥🔥");
    // print finish time as hour:minute
    System.out.println("Finished at " + java.time.LocalTime.now().withSecond(0).withNano(0));
  }

  private static int runGatlingInFork(String[] gatlingArgs) {
    try {
      String javaExe = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
      String classpath = System.getProperty("java.class.path");

      List<String> cmd = new ArrayList<>();
      cmd.add(javaExe);
      cmd.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
      cmd.add("-cp");
      cmd.add(classpath);
      cmd.add("io.gatling.app.Gatling");
      cmd.addAll(Arrays.asList(gatlingArgs));

      Process process = new ProcessBuilder(cmd)
          .inheritIO()
          .start();
      return process.waitFor();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Failed to launch Gatling in forked JVM", e);
    }
  }

  private static void waitForApp() {
    System.out.print("Wait for app to start ");
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
      System.out.println("✅Cleared Glowroot data at localhost:4000");
    } catch (IOException | InterruptedException e) {
      System.out.println("⚠️WARN: Could not clear Glowroot data. Not started on :4000?");
      // -javaagent:/Users/victorrentea/workspace/glowroot/glowroot.jar
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
