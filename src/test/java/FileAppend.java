import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("Press Enter to start writing a file. PID: " + ProcessHandle.current().pid());
    System.in.read();
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
    System.out.println("Took " + (t1 - t0) / 1000f + " s to write a file of size " + Files.size(p) / 1024 + " KB");
  }

  static int c = 0;
  static Callable<String> work(int workNo) {
    return () -> {
      int nItems = N_WORK_ITEMS / N_THREADS;
      for (int i = 0; i < nItems; i++) {
        synchronized (FileAppend.class) {
          String s = "ab" + (c++) % 10;
          Files.writeString(p, s, CREATE, APPEND);
        }
      }
      System.out.println("Work #" + workNo + " done");
      return "done";
    };
  }

}
