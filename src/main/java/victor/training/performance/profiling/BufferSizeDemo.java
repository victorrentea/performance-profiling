package victor.training.performance.profiling;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Port of Andrei Pangin's demo3 (apangin/java-profiling-presentation).
 *
 * Reading a small file with a 31 MB buffer vs a 32 MB buffer. The Java
 * code is identical, only the buffer size differs by 1 MB — yet the
 * 32 MB version is several times slower.
 *
 * Why: FileInputStream.read(byte[]) hands the buffer to native code,
 * which calls malloc() for a scratch region of the requested size.
 * With MALLOC_MMAP_THRESHOLD_=33554432 (32 MB), glibc's malloc routes
 *   - 31 MB → main heap (sbrk-grown, reused across iterations)
 *   - 32 MB → mmap + munmap on every call (faults fresh zero pages)
 *
 *   JFR             → both runs show only FileInputStream.read on top.
 *   async-profiler  → 32 MB run reveals __GI___mmap64 / __munmap /
 *                     copy_to_user / clear_page frames that JFR cannot.
 *
 * The file is intentionally tiny (4 KB) so the read syscall is cheap
 * and the alloc/free cost dominates the runtime.
 *
 * Run:
 *   java BufferSizeDemo            # both modes
 *   java BufferSizeDemo fast       # only 31 MB
 *   java BufferSizeDemo slow       # only 32 MB
 */
public class BufferSizeDemo {
  // File size = 31 MB so both buffers transfer the same 31 MB per iter.
  // What differs is where those bytes are written: into a stable heap region
  // (31 MB malloc, no faults after warmup) or into freshly mmap'd anonymous
  // pages (32 MB malloc → kernel zero-faults every page every iter).
  private static final int FILE_SIZE = 31 * 1024 * 1024;
  private static final int WARMUP = 50;
  private static final int ITERATIONS = 1500;
  private static final Path FILE = Path.of(System.getProperty("java.io.tmpdir"), "buffer-demo.bin");

  public static void main(String[] args) throws IOException {
    ensureFile();
    String mode = args.length > 0 ? args[0] : "both";
    if (mode.equals("fast") || mode.equals("both")) bench("31 MB", 31 * 1024 * 1024);
    if (mode.equals("slow") || mode.equals("both")) bench("32 MB", 32 * 1024 * 1024);
  }

  private static void bench(String label, int bufSize) throws IOException {
    for (int i = 0; i < WARMUP; i++) readFile(bufSize);
    long t0 = System.nanoTime();
    for (int i = 0; i < ITERATIONS; i++) readFile(bufSize);
    long ms = (System.nanoTime() - t0) / 1_000_000;
    System.out.printf("%s buffer × %d iters: %d ms (%.2f ms/iter)%n",
        label, ITERATIONS, ms, ms / (double) ITERATIONS);
  }

  private static void readFile(int bufSize) throws IOException {
    byte[] buf = new byte[bufSize];
    try (FileInputStream in = new FileInputStream(FILE.toFile())) {
      while (in.read(buf) > 0) {
        // discard
      }
    }
  }

  private static void ensureFile() throws IOException {
    if (Files.exists(FILE) && Files.size(FILE) == FILE_SIZE) return;
    System.out.println("Creating " + FILE_SIZE + "-byte test file at " + FILE);
    // Must contain real non-zero bytes — a sparse (zero) file would let the
    // kernel short-circuit copy_to_user via ZERO_PAGE sharing, masking the
    // mmap cliff entirely.
    byte[] chunk = new byte[1 << 20];
    java.util.Arrays.fill(chunk, (byte) 0xA5);
    try (RandomAccessFile raf = new RandomAccessFile(FILE.toFile(), "rw")) {
      raf.setLength(0);
      long remaining = FILE_SIZE;
      while (remaining > 0) {
        int n = (int) Math.min(remaining, chunk.length);
        raf.write(chunk, 0, n);
        remaining -= n;
      }
    }
  }
}
