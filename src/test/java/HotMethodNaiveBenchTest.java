import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;

// in real life, microbenchmark using  Java Measuring Harness (JMH)
public class HotMethodNaiveBenchTest {
  @Test
  void fast() {
    Set<Integer> hashSet = IntStream.range(0, 100_000).boxed().collect(toSet()); // returns a HashSet
    List<Integer> list = IntStream.range(0, 99_999).boxed().toList();

    hashSet.removeAll(list); // 26 ms
  }

  @Test
  void slow() {
    Set<Integer> hashSet = IntStream.range(0, 100_000).boxed().collect(toSet()); // returns a HashSet
    List<Integer> list = IntStream.range(0, 99_999 + 5).boxed().toList();
    Set<Integer> secondSet = new HashSet<>(list); // time/space tradeoff
    hashSet.removeAll(secondSet);// 41 ms vs 10.341 ms (with a list)
  }
}
