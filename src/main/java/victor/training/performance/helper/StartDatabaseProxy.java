package victor.training.performance.helper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

public class StartDatabaseProxy {
  private final String remoteHost;
  private final int remotePort;
  private final int port;
  private final int delayMillis;

  public StartDatabaseProxy(String remoteHost, int remotePort, int port, int delayMillis) {
    this.remoteHost = remoteHost;
    this.remotePort = remotePort;
    this.port = port;
    this.delayMillis = delayMillis;
  }

  public static void main(String[] args) throws IOException {
    var remoteHost = System.getProperty("remoteHost", "localhost");
    var remotePort = Integer.parseInt(System.getProperty("remotePort", "9092"));
    var port = Integer.parseInt(System.getProperty("port", "19092"));
    var delayMillis = Integer.parseInt(System.getProperty("delayMillis", "5"));

    new StartDatabaseProxy(remoteHost, remotePort, port, delayMillis).run();
  }

  public void run() {
    System.out.println("Proxying port " + port + " with delay " + delayMillis + "ms to remote " + remoteHost + ":" + remotePort);
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Listening ...");
      while (true) {
        Socket socket = serverSocket.accept();
        Thread thread = new Thread(new ProxyConnection(socket));
        thread.start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @RequiredArgsConstructor
  private class ProxyConnection implements Runnable {
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

      System.out.println("Proxying " + clientsocket.getInetAddress().getHostName() + ":" + clientsocket.getPort() + " <-> " + serverConnection.getInetAddress().getHostName() + ":" + serverConnection.getPort());

      new Thread(new CopyDataTask(clientsocket, serverConnection)).start();
      new Thread(new CopyDataTask(serverConnection, clientsocket)).start();
      new Thread(() -> {
        while (true) {
          if (clientsocket.isClosed()) {
            System.out.println("client socket closed: " + clientsocket.getInetAddress().getHostName() + ":" + clientsocket.getPort());
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
          System.out.println("closing remote host connection " + serverConnection.getInetAddress().getHostName() + ":" + serverConnection.getPort());
          serverConnection.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @RequiredArgsConstructor
  private class CopyDataTask implements Runnable {
    private final Socket in;
    private final Socket out;

    @Override
    public void run() {
      System.out.println("Copy data " + in.getInetAddress().getHostName() + ":" + in.getPort() + " --> " + out.getInetAddress().getHostName() + ":" + out.getPort());
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
