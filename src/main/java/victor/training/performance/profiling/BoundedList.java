package victor.training.performance.profiling;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BoundedList<T> {
  private final List<T> list = new ArrayList<>();
  private final int maxSize;


  public BoundedList(int maxSize) {
    this.maxSize = maxSize;
  }

  public synchronized List<T> getACopy() {
    return new ArrayList<>(list);
  }

  public synchronized void add(T element) {
    list.remove(element);
    list.add(element);
    while (list.size() > maxSize) list.remove(0);
  }
}
