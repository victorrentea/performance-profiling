import com.github.noconnor.junitperf.JUnitPerfInterceptor;
import com.github.noconnor.junitperf.JUnitPerfTest;
import com.github.noconnor.junitperf.JUnitPerfTestRequirement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;

// in real life, microbenchmark using  Java Measuring Harness (JMH)
@ExtendWith(JUnitPerfInterceptor.class)
public class HotMethodNaiveBenchTest {
  @Test
//  @JUnitPerfTest(durationMs = 125_000, warmUpMs = 10_000, maxExecutionsPerSecond = 1000)
//  @JUnitPerfTestRequirement(percentiles = "90:7,95:7,98:7,99:8", executionsPerSec = 1000, allowedErrorPercentage = 0.10)

//  @JUnitPerfTest(warmUpMs = 1000)
//  @JUnitPerfTestRequirement(meanLatency = 1)
  void fast() {
    Set<Integer> hashSet = IntStream.range(0, 100_000).boxed()
        .collect(toSet()); // returns a HashSet
    List<Integer> list = IntStream.range(0, 101_000).boxed().toList();

    hashSet.removeAll(list);
  }

}
