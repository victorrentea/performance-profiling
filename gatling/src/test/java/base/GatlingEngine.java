package base;

import io.gatling.app.Gatling;
import io.gatling.core.config.GatlingPropertiesBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GatlingEngine {
  public static void main(String[] args) {
    GatlingPropertiesBuilder props = new GatlingPropertiesBuilder()
            .resourcesDirectory(mavenResourcesDirectory().toString())
            .resultsDirectory(resultsDirectory().toString())
            .binariesDirectory(mavenBinariesDirectory().toString());
    Gatling.fromMap(props.build());

  }

  public static void startClass(Class<?> clazz) {
    GatlingPropertiesBuilder props = new GatlingPropertiesBuilder()
            .resourcesDirectory(mavenResourcesDirectory().toString())
            .resultsDirectory(resultsDirectory().toString())
            .binariesDirectory(mavenBinariesDirectory().toString())
            .simulationClass(clazz.getCanonicalName());

    clearGlowrootData();
    Gatling.fromMap(props.build());

  }

  private static void clearGlowrootData()  {
    try {
      URI uri = URI.create("http://localhost:4000/backend/admin/delete-all-stored-data");
      HttpRequest postRequest = HttpRequest.newBuilder().POST(BodyPublishers.ofString("{}")).uri(uri).build();
      HttpClient.newHttpClient().send(postRequest, BodyHandlers.discarding());
      System.out.println("Deleted Glowroot data at localhost:4000!");
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
    return projectRootDir()    .resolve("src").resolve("test");
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
