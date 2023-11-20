package victor.training.performance.profiling.util;

import java.util.ArrayList;
import java.util.List;

public class BoundedQueue<T> {
  private final List<T> data = new ArrayList<>();
  private final int maxSize;

  public BoundedQueue(int maxSize) {
    this.maxSize = maxSize;
  }

  public synchronized void add(T e) {
    data.remove(e);
    data.add(e);
    while (data.size() > maxSize) data.remove(0);
  }
  public synchronized List<T> getContents() {
    return new ArrayList<>(data);
  }
}
