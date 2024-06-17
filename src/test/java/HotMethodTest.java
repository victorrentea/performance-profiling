import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;

public class HotMethodTest {
  @Test
  void naive() { // in real life, microbenchmark using  Java Measuring Harness (JMH)
    Set<Integer> hashSet = IntStream.range(0, 100_000).boxed()
        .collect(toSet()); // returns a HashSet
    List<Integer> list = IntStream.range(0, 100_000).boxed().toList();

    hashSet.removeAll(list);
  }
}
