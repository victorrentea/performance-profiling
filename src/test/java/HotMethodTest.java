import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toSet;

// in real life, microbenchmark using  Java Measuring Harness (JMH)
@Slf4j
public class HotMethodTest {
  @Test
  void fast() {
    Set<Integer> hashSet = IntStream.range(0, 100_000).boxed().collect(toSet()); // returns a HashSet
    List<Integer> list = IntStream.range(0, 99_999+2).boxed().toList();
    hashSet.removeAll(list); // 60 ms -> 10.000 ms
    // CPU hotspot;
  }

  @Test
  void slow() {
    Set<Integer> hashSet = IntStream.range(0, 100_000).boxed()
        .collect(toSet()); // returns a HashSet
    List<Integer> list = IntStream.range(0, 100_000).boxed().toList();

    hashSet.removeAll(list);
  }
}
