import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;

// in real life, microbenchmark using  Java Measuring Harness (JMH)
public class HotMethodTest {
  @Test
  void fast() {
    Set<Integer> hashSet = IntStream.range(0, 100_000).boxed()
        .collect(toSet()); // returns a HashSet
    List<Integer> list = IntStream.range(0, 99_999).boxed().toList();

    hashSet.removeAll(list);
  }

  @Test
  void slow() {
    Set<Integer> hashSet = IntStream.range(0, 100_000).boxed()
        .collect(toSet()); // returns a HashSet
    List<Integer> list = IntStream.range(100000, 100000+ 99_999+1).boxed().toList();

    list.forEach(hashSet::remove);
    // or worse :hashSet.removeAll(new HashSet<>(list));
  }
}
