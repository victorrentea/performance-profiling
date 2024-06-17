package victor.training.performance.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StartDatabaseProxy {
  private static String remoteHost;
  private static int remotePort;
  private static int port;
  private static int delayMillis;

  public static void main(String[] args) throws IOException {
    remoteHost = System.getProperty("remoteHost", "localhost");
    remotePort = Integer.parseInt(System.getProperty("remotePort", "9092"));
    port = Integer.parseInt(System.getProperty("port", "19092"));
    delayMillis = Integer.parseInt(System.getProperty("delayMillis", "5"));

    log.info("Proxying port {} with delay {}ms to remote {}:{}", port, delayMillis, remoteHost, remotePort);
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      log.info("Listening ...");
      while (true) {
        Socket socket = serverSocket.accept();
        Thread thread = new Thread(new ProxyConnection(socket));
        thread.start();
      }
    }
  }

  @Slf4j
  @RequiredArgsConstructor
  private static class ProxyConnection implements Runnable {
    private final Socket clientsocket;
    private Socket serverConnection = null;

    @Override
    public void run() {
      try {
        serverConnection = new Socket(remoteHost, remotePort);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }

      log.info("Proxying {}:{} <-> {}:{}", clientsocket.getInetAddress().getHostName(), clientsocket.getPort(), serverConnection.getInetAddress().getHostName(), serverConnection.getPort());

      new Thread(new CopyDataTask(clientsocket, serverConnection)).start();
      new Thread(new CopyDataTask(serverConnection, clientsocket)).start();
      new Thread(() -> {
        while (true) {
          if (clientsocket.isClosed()) {
            log.info("client socket ({}:{}) closed", clientsocket.getInetAddress().getHostName(), clientsocket.getPort());
            closeServerConnection();
            break;
          }

          try {
            Thread.sleep(1000);
          } catch (InterruptedException ignored) {
          }
        }
      }).start();
    }

    private void closeServerConnection() {
      if (serverConnection != null && !serverConnection.isClosed()) {
        try {
          log.info("closing remote host connection {}:{}", serverConnection.getInetAddress().getHostName(), serverConnection.getPort());
          serverConnection.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Slf4j
  @RequiredArgsConstructor
  private static class CopyDataTask implements Runnable {
    private final Socket in;
    private final Socket out;

    @Override
    public void run() {
      log.info("Copy data {}:{} --> {}:{}", in.getInetAddress().getHostName(), in.getPort(), out.getInetAddress().getHostName(), out.getPort());
      try {
        InputStream inputStream = in.getInputStream();
        OutputStream outputStream = out.getOutputStream();

        if (inputStream == null || outputStream == null) {
          return;
        }

        byte[] reply = new byte[40960];
        int bytesRead;
        while (-1 != (bytesRead = inputStream.read(reply))) {
          outputStream.write(reply, 0, bytesRead);
          TimeUnit.MILLISECONDS.sleep(delayMillis);
        }
      } catch (SocketException ignored) {
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        try {
          in.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
