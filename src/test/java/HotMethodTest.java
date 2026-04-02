import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;

// ⚠️Only microbenchmark sub-ms code using Java Measuring Harness (JMH)
@TestMethodOrder(MethodName.class)
public class HotMethodTest {
  Set<Integer> hashSet = range(0, 100_000).boxed().collect(toSet());

  @Test
  void a_one() {
    hashSet.remove(1);
  }

  @Test
  void b_fast_99k() {
    List<Integer> list = range(0, 99_999).boxed().toList();

    hashSet.removeAll(list);
  }

  @Test
  void c_slow_100k() {
    List<Integer> list = range(0, 100_000).boxed().toList();

    hashSet.removeAll(list);
  }
}
