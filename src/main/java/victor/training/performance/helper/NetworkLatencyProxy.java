package victor.training.performance.helper;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

public class NetworkLatencyProxy {
  private String remoteHost = "localhost";
  private int remotePort = 9092;
  private int port = 19092;
  private int delayMillis = 3;

  public NetworkLatencyProxy() {
  }

  public NetworkLatencyProxy(String remoteHost, Integer remotePort, Integer port, Integer delayMillis) {
    if (remoteHost != null) this.remoteHost = remoteHost;
    if (remotePort != null) this.remotePort = remotePort;
    if (port != null) this.port = port;
    if (delayMillis != null) this.delayMillis = delayMillis;
  }

  public static void main(String[] args) throws IOException {
    var remoteHost = System.getProperty("remoteHost", null);
    var remotePort = Integer.parseInt(System.getProperty("remotePort", null));
    var port = Integer.parseInt(System.getProperty("port", null));
    var delayMillis = Integer.parseInt(System.getProperty("delayMillis", null));

    new NetworkLatencyProxy(remoteHost, remotePort, port, delayMillis).run();
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
