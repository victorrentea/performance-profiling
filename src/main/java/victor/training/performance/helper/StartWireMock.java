package victor.training.performance.helper;

import com.github.tomakehurst.wiremock.standalone.WireMockServerRunner;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

public class StartWireMock {
  public static void main(String[] args) throws IOException {
    File rootFolder = new File("wiremock");
    File mappingsFolder = new File(rootFolder, "mappings");
    System.out.println("*.json mappings stubs expected at " + mappingsFolder.getAbsolutePath());
    if (!mappingsFolder.isDirectory()) {
      throw new IllegalArgumentException("Not a folder: " + mappingsFolder.getAbsolutePath());
    }

    CompletableFuture.runAsync(() ->
            System.out.println("You should see a JSON at http://localhost:9999/loan-comments/1"),
        CompletableFuture.delayedExecutor(4, SECONDS));

    new WireMockServerRunner().run(
            "--port", "9999",
            "--root-dir", rootFolder.getAbsolutePath(),
            "--global-response-templating", // UUID
            "--async-response-enabled=true" // enable Wiremock to not bottleneck on heavy load
    );
  }
}
