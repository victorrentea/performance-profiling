package victor.training.performance;

import com.github.tomakehurst.wiremock.standalone.WireMockServerRunner;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class StartWireMock {
  public static void main(String[] args) throws IOException {
    File rootFolder = new File(".", "wiremock");
    File mappingsFolder = new File(rootFolder, "mappings");
    System.out.println("*.json mappings stubs expected at " + mappingsFolder.getAbsolutePath());

    CompletableFuture.runAsync(() -> WatchFolder.watchFolder(mappingsFolder, () ->
      HttpClients.createDefault().execute(new HttpPost("http://localhost:9999/__admin/mappings/reset"))
    ));

    WireMockServerRunner.main(
            "--port", "9999",
            "--root-dir", rootFolder.getAbsolutePath(),
            "--global-response-templating", // UUID
            "--async-response-enabled=true" // enable Wiremock to not bottleneck on heavy load
    );
  }
}
