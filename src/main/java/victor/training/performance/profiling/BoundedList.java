package victor.training.performance.profiling;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BoundedList {
  private final List<Long> recentLoanStatusQueried = new ArrayList<>();
//  private final List<Long> recentLoanStatusQueried = new CopyOnWriteArrayList<>(); // TODO
  private final int maxSize;

  public BoundedList(int maxSize) {
    this.maxSize = maxSize;
  }

  public synchronized void add(Long loanId) {
    recentLoanStatusQueried.remove(loanId); // BUG#7235 - avoid duplicates in list
    recentLoanStatusQueried.add(loanId);
    while (recentLoanStatusQueried.size() > maxSize) {
      recentLoanStatusQueried.remove(0);
    }
  }

  public synchronized List<Long> getCopy() {
    return new ArrayList<>(recentLoanStatusQueried);
  }
}
