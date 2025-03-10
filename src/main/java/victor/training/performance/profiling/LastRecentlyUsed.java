package victor.training.performance.profiling;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * methods of this class are thread safe
 */
public class LastRecentlyUsed {
  private final List<Long> recentLoanStatusQueried = new ArrayList<>();
  private final ReentrantLock lock = new ReentrantLock();

  public void updateLastUsed(Long loanId) {
    //    synchronized (recentLoanStatusQueried) {
    lock.lock();
    try {
      recentLoanStatusQueried.remove(loanId); // BUG#7235 - avoid duplicates in list
      recentLoanStatusQueried.add(loanId);
      while (recentLoanStatusQueried.size() > 10) recentLoanStatusQueried.remove(0);
    } finally {
      lock.unlock();
    }
  }

  public List<Long> get() {
    lock.lock();
    try {
      return new ArrayList<>(recentLoanStatusQueried);
    } finally {
      lock.unlock();
    }
  }
  // public void evil() {
  ////    synchronized (recentLoanStatusQueried) {
  //    lock.lock();
  //    try {
  //      recentLoanStatusQueried.add(1L); // other thread(s) doing this cannot RACE with getLoanStatus to change the list
  //    } finally {
  //      lock.unlock();
  //    }
  //  }
}
