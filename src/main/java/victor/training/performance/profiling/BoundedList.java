package victor.training.performance.profiling;

import java.util.ArrayList;
import java.util.List;

// thread-safe object
public class BoundedList<T> {
  private final List<T> list = new ArrayList<>();

  public synchronized void add(T e) {
    list.remove(e); // remove it BUG#7235 - avoid duplicates in list
    list.add(e); // to add it again at the end
    while (list.size() > 10) list.remove(0); // ensure list size <= 10

  }
  public synchronized List<T> getAll() {
    return new ArrayList<>(list);
  }
}
