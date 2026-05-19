package victor.training.performance.profiling;

/**
 * Reproduces a long Time-To-Safepoint (TTSP):
 *  - N worker threads spin in a tight counted loop (int index, no calls inside)
 *    so the JIT-compiled code contains *no* safepoint polls inside the loop body.
 *  - One allocator thread churns big arrays to force the GC to request a safepoint.
 *  - The GC has to wait for the workers to finish (or exit) the loop before it can start.
 *
 * Run it like this (the flags are the point — without them, JDK 21's loop strip mining
 * inserts safepoint polls every 1000 iterations and you won't see the problem):
 *
 *   javac -d /tmp/ttsp src/main/java/victor/training/performance/profiling/TtspDemo.java
 *   java -cp /tmp/ttsp \
 *     -Xmx256m \
 *     -XX:-UseCountedLoopSafepoints \
 *     -XX:LoopStripMiningIter=0 \
 *     -Xlog:safepoint*=info:file=safepoint.log:time,uptime,tid \
 *     -XX:+SafepointTimeout \
 *     -XX:SafepointTimeoutDelay=200 \
 *     victor.training.performance.profiling.TtspDemo
 *
 * Then:
 *   grep "Reaching safepoint" safepoint.log | sort -t: -k4 -n | tail
 *   # and look at stderr for "Threads which did not reach the safepoint"
 *
 * To prove the fix works, re-run WITHOUT the two -XX:...CountedLoop flags
 * (or add -XX:+UseCountedLoopSafepoints): TTSP collapses back to microseconds.
 */
public class TtspDemo {

  private static volatile long sink;

  public static void main(String[] args) throws InterruptedException {
    int workers = Integer.getInteger("workers", Math.max(2, Runtime.getRuntime().availableProcessors() - 1));
    int iterations = Integer.getInteger("iterations", 1_500_000_000);
    int rounds = Integer.getInteger("rounds", 20);

    System.out.printf("workers=%d  iterations=%,d  rounds=%d  jvm=%s%n",
        workers, iterations, rounds, System.getProperty("java.version"));

    Thread allocator = new Thread(TtspDemo::allocateForever, "allocator");
    allocator.setDaemon(true);
    allocator.start();

    long t0 = System.nanoTime();
    for (int r = 0; r < rounds; r++) {
      Thread[] threads = new Thread[workers];
      for (int i = 0; i < workers; i++) {
        threads[i] = new Thread(() -> hotCountedLoop(iterations), "worker-" + i);
        threads[i].start();
      }
      for (Thread t : threads) t.join();
      System.out.printf("round %2d done at +%.2fs  sink=%d%n",
          r, (System.nanoTime() - t0) / 1e9, sink);
    }
  }

  // Hot counted loop: `int` index, pure arithmetic, no method calls, no allocations.
  // With -XX:-UseCountedLoopSafepoints / LoopStripMiningIter=0, the JIT emits NO
  // safepoint polls inside this loop — the GC must wait for it to finish.
  private static void hotCountedLoop(int iterations) {
    long acc = sink;
    for (int j = 0; j < iterations; j++) {
      acc = acc * 31 + j;
      acc ^= acc >>> 17;
      acc *= 0x9E3779B97F4A7C15L;
    }
    sink = acc;
  }

  // Triggers GC by allocating large byte arrays continuously.
  private static void allocateForever() {
    while (!Thread.currentThread().isInterrupted()) {
      byte[] big = new byte[8 * 1024 * 1024];
      sink ^= big.length;
    }
  }
}
