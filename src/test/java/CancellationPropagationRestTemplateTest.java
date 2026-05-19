import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Same scenario as {@link CancellationPropagationTest}, but the HTTP client is
 * Spring's {@link RestTemplate}.
 *
 *  WHAT CHANGES vs. java.net.http.HttpClient.sendAsync():
 *  ----------------------------------------------------------
 *  1. RestTemplate is purely BLOCKING. It does not expose any CompletableFuture
 *     of its own. To get a CompletableFuture at all we must wrap each call
 *     in CompletableFuture.supplyAsync(...) — the future is no longer "owned"
 *     by the HTTP engine, it just wraps a worker thread doing a blocking call.
 *
 *  2. CompletableFuture.cancel(true) does NOT interrupt the worker thread.
 *     From the JDK javadoc: "The mayInterruptIfRunning parameter has no effect
 *     in this implementation because interrupts are not used to control
 *     processing." So the boolean is a lie.
 *
 *  3. Even if you interrupted the worker thread, the default RestTemplate
 *     transport (SimpleClientHttpRequestFactory → HttpURLConnection) ignores
 *     Thread.interrupt() while it's blocked in the kernel reading the socket.
 *
 *  Net effect: the sibling future flips to "cancelled" instantly, but the
 *  underlying TCP connection stays open — the resource we wanted to save is
 *  NOT saved. That is exactly what this test asserts (and is the reason this
 *  pattern works with HttpClient.sendAsync but not with RestTemplate).
 *
 *  IS THERE AN ASYNC RestTemplate?
 *  ----------------------------------------------------------
 *  AsyncRestTemplate existed but has been deprecated since Spring 5.0 (Sept 2017)
 *  and returns ListenableFuture, not CompletableFuture. Spring's official
 *  guidance is to use WebClient (from spring-webflux) for non-blocking HTTP.
 *  WebClient is reactive and DOES propagate cancellation down to the
 *  network — disposing the subscription closes the socket, similar to
 *  HttpClient.sendAsync().
 *
 *  TL;DR: with vanilla RestTemplate the cancellation flow is cosmetic;
 *  use HttpClient.sendAsync (JDK) or WebClient (Spring) if you need it
 *  to actually free the socket.
 */
class CancellationPropagationRestTemplateTest {

  @Test
  void restTemplateCancellationDoesNotCloseTheSocket() throws Exception {
    AtomicBoolean closedByClient = new AtomicBoolean(false);
    CountDownLatch slowRequestReceived = new CountDownLatch(1);
    AtomicReference<Socket> slowAcceptedSocket = new AtomicReference<>();

    // ---------- SLOW server: accepts the request, then never answers. ----------
    ServerSocket slowServer = new ServerSocket(0);
    Thread slowThread = new Thread(() -> {
      try {
        Socket s = slowServer.accept();
        slowAcceptedSocket.set(s);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
          // consume request line + headers
        }
        slowRequestReceived.countDown();
        System.out.println("[slow-server] request received, NOT responding — waiting...");
        // Block. read() returns -1 only if the *remote* peer sends FIN.
        int b = s.getInputStream().read();
        if (b == -1) {
          closedByClient.set(true);
          System.out.println("[slow-server] client closed the TCP connection (read() == -1)");
        }
      } catch (IOException e) {
        // SocketException here means WE closed it from the cleanup at the end —
        // NOT a client close. Don't set the flag.
        System.out.println("[slow-server] " + e + " (likely local cleanup, not a client close)");
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

    RestTemplate rest = new RestTemplate();
    String slowUrl = "http://localhost:" + slowServer.getLocalPort() + "/slow";
    String failUrl = "http://localhost:" + failServer.getLocalPort() + "/fail";

    // (1) Slow call — RestTemplate is blocking, so the only way to get a
    //     CompletableFuture is to wrap it in supplyAsync. The future here
    //     wraps a worker thread, it does NOT own the HTTP engine.
    CompletableFuture<String> slowCall = CompletableFuture.supplyAsync(
        () -> rest.getForObject(slowUrl, String.class));

    assertTrue(slowRequestReceived.await(2, TimeUnit.SECONDS),
        "slow server should have received the request before the failure fires");

    // (2) Fail call — also wrapped. RestTemplate throws HttpServerErrorException
    //     on 5xx, so this future completes exceptionally on its own.
    CompletableFuture<String> failCall = CompletableFuture.supplyAsync(
        () -> rest.getForObject(failUrl, String.class));

    // ---------- Same sibling-cancellation wiring as the HttpClient test ----------
    slowCall.whenComplete((r, e) -> {
      if (e != null) {
        System.out.println("[wiring] slowCall failed → cancelling failCall");
        failCall.cancel(true);
      }
    });
    failCall.whenComplete((r, e) -> {
      if (e != null) {
        System.out.println("[wiring] failCall failed → cancelling slowCall: " + e);
        // This flips slowCall to "cancelled" instantly, BUT the worker thread
        // running rest.getForObject() keeps running, and the TCP socket stays open.
        slowCall.cancel(true);
      }
    });

    // Give the race plenty of time to resolve. failCall dies fast, cancellation
    // is wired, slowCall is "cancelled" — but the socket should still be alive.
    Thread.sleep(1500);

    // The future LOOKS cancelled...
    assertTrue(slowCall.isCancelled(),
        "slowCall CF flips to cancelled state immediately");

    // ...but nothing actually happened on the wire:
    assertFalse(closedByClient.get(),
        "RestTemplate worker is still stuck reading the socket — connection NOT closed");
    assertTrue(slowThread.isAlive(),
        "slow server is still blocked on read() because the client never closed");

    System.out.println("[main] PROOF: slowCall.isCancelled()=" + slowCall.isCancelled()
        + " but closedByClient=" + closedByClient.get()
        + " → cancellation did NOT free the network socket");

    // ---------- Cleanup ----------
    // Close the accepted socket from this thread to unblock the slow server
    // thread (and incidentally also the orphaned RestTemplate worker, which
    // will now see EOF and throw).
    Socket s = slowAcceptedSocket.get();
    if (s != null) s.close();
    slowServer.close();
    failServer.close();
  }
}
