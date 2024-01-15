package victor.training.performance.extra;

import com.github.tomakehurst.wiremock.standalone.WireMockServerRunner;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class StartWireMock {
  public static void main(String[] args) throws IOException {
    File rootFolder = new File(".", "wiremock");
    File mappingsFolder = new File(rootFolder, "mappings");
    System.out.println("*.json mappings stubs expected at " + mappingsFolder.getAbsolutePath());

    CompletableFuture.runAsync(() -> System.out.println("You should see a JSON at http://localhost:9999/loan-comments/1"),
        CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));

    new WireMockServerRunner().run(
            "--port", "9999",
            "--root-dir", rootFolder.getAbsolutePath(),
            "--global-response-templating", // UUID
            "--async-response-enabled=true" // enable Wiremock to not bottleneck on heavy load
    );
  }
}
