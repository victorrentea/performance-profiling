import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pangin's Demo 1 — StringBuilder safepoint bias.
 *
 * Hot work: deleteCharAt(0) on a 1M-char StringBuilder shifts ~1M chars left
 * via System.arraycopy() each call.
 *
 * - async-profiler (CPU/itimer) samples on a kernel timer, sees arraycopy.
 * - JFR's jdk.ExecutionSample only fires at safepoint polls. The arraycopy
 *   intrinsic has no polls, so JFR samples land in surrounding methods --
 *   or, more often, nowhere at all.
 *
 * Open the two generated HTML flamegraphs side-by-side to see the bias.
 *
 * Requires async-profiler installed at ~/workspace/async-profiler/.
 */
public class StringBuilderAsyncProfilerTest {

  static final int LENGTH = 1_000_000;
  static final int WARMUP_S = 3;
  static final int PROFILE_S = 10;
  static final Path ASPROF_HOME = Path.of(System.getProperty("user.home"), "workspace/async-profiler");
  static final Path ASPROF = ASPROF_HOME.resolve("bin/asprof");
  static final Path JFRCONV = ASPROF_HOME.resolve("bin/jfrconv");
  static final Path OUT_DIR = Path.of("target/async-profiler/out");

  @Test
  void jfrVsAsyncProfiler() throws Exception {
    assertThat(ASPROF).as("async-profiler must be installed").exists();
    Files.createDirectories(OUT_DIR);

    StringBuilder sb = new StringBuilder(LENGTH);
    for (int i = 0; i < LENGTH; i++) sb.append('a');

    System.out.println("Warming up JIT for " + WARMUP_S + "s ...");
    churn(sb, WARMUP_S);

    // -------- JFR run (jdk.ExecutionSample forced to 10ms to match async-profiler) --------
    Path jfrFile = OUT_DIR.resolve("demo1-jfr.jfr");
    try (Recording rec = new Recording(Configuration.getConfiguration("profile"))) {
      rec.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(10));
      rec.setDestination(jfrFile);
      rec.start();
      System.out.println("JFR profiling for " + PROFILE_S + "s ...");
      churn(sb, PROFILE_S);
      rec.stop();
    }

    // -------- async-profiler run (cpu / itimer on macOS, JFR output) --------
    Path asyncFile = OUT_DIR.resolve("demo1-async.jfr");
    long pid = ProcessHandle.current().pid();
    Process asprof = new ProcessBuilder(
        ASPROF.toString(),
        "-d", String.valueOf(PROFILE_S),
        "-e", "cpu",
        "-i", "10ms",
        "-o", "jfr",
        "-f", asyncFile.toString(),
        String.valueOf(pid))
        .redirectErrorStream(true)
        .inheritIO()
        .start();
    System.out.println("async-profiler attached, profiling for " + PROFILE_S + "s ...");
    churn(sb, PROFILE_S);
    int rc = asprof.waitFor();
    assertThat(rc).as("asprof exit code").isZero();

    // -------- render HTML flamegraphs for both runs --------
    Path jfrHtml = renderFlamegraph(jfrFile);
    Path asyncHtml = renderFlamegraph(asyncFile);

    System.out.println();
    System.out.println("Flamegraphs (open in browser):");
    System.out.println("  JFR             : " + jfrHtml.toAbsolutePath());
    System.out.println("  async-profiler  : " + asyncHtml.toAbsolutePath());
  }

  private static void churn(StringBuilder sb, int seconds) {
    long deadline = System.nanoTime() + seconds * 1_000_000_000L;
    while (System.nanoTime() < deadline) {
      for (int i = 0; i < 1_000; i++) {
        sb.deleteCharAt(0);
        sb.append('a');
      }
    }
  }

  private static Path renderFlamegraph(Path jfr) throws Exception {
    String name = jfr.getFileName().toString().replaceFirst("\\.jfr$", ".html");
    Path html = jfr.resolveSibling(name);
    Process p = new ProcessBuilder(
        JFRCONV.toString(), "-o", "html", jfr.toString(), html.toString())
        .redirectErrorStream(true)
        .inheritIO()
        .start();
    int rc = p.waitFor();
    assertThat(rc).as("jfrconv exit code for " + jfr).isZero();
    return html;
  }
}
