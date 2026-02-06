import lombok.extern.slf4j.Slf4j;
import victor.training.performance.profiling.util.FileHashUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

@Slf4j
public class FileAppend {
  static Path p = Path.of("out.txt");
  static int N_WORK_ITEMS = 200_000;
  static int N_THREADS = 4;

  public static void main(String[] args) throws IOException, InterruptedException, NoSuchAlgorithmException {
    System.out.println("Starting async-profiler on this process...");
    long pid = ProcessHandle.current().pid();
    String asprofCmd = System.getProperty("user.home") + "/workspace/async-profiler/bin/asprof -d 30 -f flamegraph.html " + pid;
    new ProcessBuilder("sh", "-c", asprofCmd)
        .inheritIO()
        .start();
    Thread.sleep(200);

    System.out.println("Start writing file...");
    p.toFile().delete();

    long t0 = currentTimeMillis();
    List<Callable<String>> tasks = IntStream.range(0, N_THREADS)
        .mapToObj(FileAppend::work)
        .toList();
    try (var executor = Executors.newCachedThreadPool()) {
      executor.invokeAll(tasks);
    }
    long t1 = currentTimeMillis();

    System.out.println("Took " + (t1 - t0) / 1000f + " s to write a file of size " +
        Files.size(p) / 1024 + " KB, of hash: " + FileHashUtil.computeShortHash(p));

    System.out.println("Flamegraph: file://" + Path.of("flamegraph.html").toAbsolutePath());
  }

  static int c = 0;

  static Callable<String> work(int workNo) {
    return () -> {
      int nItems = N_WORK_ITEMS / N_THREADS;
      for (int i = 0; i < nItems; i++) {
        synchronized (FileAppend.class) {
          Files.writeString(p, "ab" + (c++) % 10, CREATE, APPEND);
        }
      }
      System.out.println("Work #" + workNo + " done");
      return "done";
    };
  }

}
