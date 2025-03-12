package victor.training.performance.profiling.util;

public class Sleep {
  public static void millis(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
