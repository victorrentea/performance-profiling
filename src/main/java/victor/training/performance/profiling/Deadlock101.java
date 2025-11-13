package victor.training.performance.profiling;

import java.util.concurrent.CompletableFuture;

public class Deadlock101 {
  public static void main(String[] args) {
    Object a = new Object(), b = new Object();
    for (int i = 0; i < 10000; i++) {
      CompletableFuture.runAsync(() -> {
        synchronized (a) {
//        try {
//          Thread.sleep(100);
//        } catch (InterruptedException e) {
//          throw new RuntimeException(e);
//        }
          synchronized (b) {
            System.out.println("AB");
          }
        }
      });
      CompletableFuture.runAsync(() -> {
        synchronized (b) {
//        try {
//          Thread.sleep(100);
//        } catch (InterruptedException e) {
//          throw new RuntimeException(e);
//        }
          synchronized (a) {
            System.out.println("BA");
          }
        }
      }).join();
    }
  }
}
