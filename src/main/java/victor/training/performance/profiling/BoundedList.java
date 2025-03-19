package victor.training.performance.profiling;

import java.util.ArrayList;
import java.util.List;

public class BoundedList {
  private final List<Long> list = new ArrayList<>();
  private static final Object lock = new Object();
  private final int maxSize;

  public BoundedList(int maxSize) {
    this.maxSize = maxSize;
  }

  public void add(Long loanId) {
    synchronized (lock) {
      list.remove(loanId); // BUG#7235 - avoid duplicates in list
      list.add(loanId);
      while (list.size() > maxSize) list.remove(0);
    }
  }
  public ArrayList<Long> getACopy() {
    synchronized (lock) {
      return new ArrayList<>(list);
    }
  }
}
