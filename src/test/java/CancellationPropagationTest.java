import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two HTTP calls run concurrently:
 *   - slowCall : started from main thread via HttpClient.sendAsync (returns CompletableFuture)
 *   - failCall : wrapped inside CompletableFuture.supplyAsync, performs sendAsync too
 *
 * If either future completes exceptionally, the sibling is cancelled.
 * Cancelling the CompletableFuture returned by java.net.http.HttpClient.sendAsync
 * propagates down and aborts the underlying TCP connection — which is exactly
 * what we want to prove. The "slow" toy-server blocks on read() and reports
 * read() == -1 the instant the client closes the socket.
 */
class CancellationPropagationTest {

  @Test
  void siblingCancellationClosesHealthyConnection() throws Exception {
    // ---------- SLOW server: accepts the request, then never answers. ----------
    // It blocks reading from the socket. When the client cancels and closes the
    // TCP connection, read() returns -1 → we know the connection was killed
    // remotely (= cancellation actually reached the kernel socket).
    AtomicBoolean slowConnectionClosedByClient = new AtomicBoolean(false);
    CountDownLatch slowRequestReceived = new CountDownLatch(1);
    CountDownLatch slowServerDone = new CountDownLatch(1);

    ServerSocket slowServer = new ServerSocket(0);
    Thread slowThread = new Thread(() -> {
      try (Socket s = slowServer.accept()) {
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
          // consume request line + headers
        }
        slowRequestReceived.countDown();
        System.out.println("[slow-server] request received, NOT responding — waiting...");
        // Block. The only way out is the client closing the TCP connection.
        int b = s.getInputStream().read();
        if (b == -1) {
          slowConnectionClosedByClient.set(true);
          System.out.println("[slow-server] client closed the TCP connection (read() == -1)");
        } else {
          System.out.println("[slow-server] unexpected byte: " + b);
        }
      } catch (IOException e) {
        // RST from client also counts as a remote close
        slowConnectionClosedByClient.set(true);
        System.out.println("[slow-server] IOException (likely RST): " + e);
      } finally {
        slowServerDone.countDown();
      }
    }, "slow-server");
    slowThread.setDaemon(true);
    slowThread.start();

    // ---------- FAIL server: accepts, immediately returns HTTP 500. ----------
    ServerSocket failServer = new ServerSocket(0);
    Thread failThread = new Thread(() -> {
      try (Socket s = failServer.accept()) {
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
          // consume headers
        }
        OutputStream out = s.getOutputStream();
        out.write(("HTTP/1.1 500 Internal Server Error\r\n"
                + "Content-Length: 0\r\n"
                + "Connection: close\r\n\r\n").getBytes());
        out.flush();
        System.out.println("[fail-server] returned 500");
      } catch (IOException e) {
        System.out.println("[fail-server] " + e);
      }
    }, "fail-server");
    failThread.setDaemon(true);
    failThread.start();

    // ---------- Client side ----------
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();

    HttpRequest slowReq = HttpRequest.newBuilder(
        URI.create("http://localhost:" + slowServer.getLocalPort() + "/slow")).build();
    HttpRequest failReq = HttpRequest.newBuilder(
        URI.create("http://localhost:" + failServer.getLocalPort() + "/fail")).build();

    // (1) Slow call: started right here on the main thread.
    CompletableFuture<HttpResponse<String>> slowCall =
        client.sendAsync(slowReq, BodyHandlers.ofString());

    // Make sure the slow request actually got out before we trigger the failure,
    // otherwise we could cancel before the connection is even open.
    assertTrue(slowRequestReceived.await(2, TimeUnit.SECONDS),
        "slow server should have received the request");

    // (2) Fail call: started from inside a CompletableFuture.
    //     500 isn't an exception by itself, so we map it to one so the
    //     future completes exceptionally — that's what triggers cancellation.
    CompletableFuture<HttpResponse<String>> failCall = CompletableFuture
        .supplyAsync(() -> client.sendAsync(failReq, BodyHandlers.ofString()))
        .thenCompose(f -> f)
        .thenApply(resp -> {
          if (resp.statusCode() >= 500) {
            throw new RuntimeException("HTTP " + resp.statusCode());
          }
          return resp;
        });

    // ---------- Sibling cancellation wiring ----------
    slowCall.whenComplete((r, e) -> {
      if (e != null) {
        System.out.println("[wiring] slowCall failed → cancelling failCall: " + e);
        failCall.cancel(true);
      }
    });
    failCall.whenComplete((r, e) -> {
      if (e != null) {
        System.out.println("[wiring] failCall failed → cancelling slowCall: " + e);
        slowCall.cancel(true);
      }
    });

    // Wait for the race to resolve. We expect failCall to die first,
    // which cancels slowCall, which closes the TCP connection to slow-server.
    try {
      CompletableFuture.anyOf(slowCall, failCall).get(5, TimeUnit.SECONDS);
    } catch (CompletionException | java.util.concurrent.ExecutionException expected) {
      System.out.println("[main] race finished with: " + expected.getCause());
    }

    // The proof: the slow server saw its TCP connection drop from the client side.
    assertTrue(slowServerDone.await(5, TimeUnit.SECONDS),
        "slow server thread should have unblocked (it only unblocks on client close)");
    assertTrue(slowConnectionClosedByClient.get(),
        "slow server should have observed the client closing the TCP connection");
    assertTrue(slowCall.isCompletedExceptionally(),
        "slowCall future should be in a failed/cancelled state");

    slowServer.close();
    failServer.close();
  }
}
