import org.checkerframework.common.value.qual.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HotMethodBenchmark {
  @Test
  void naive() {
    HashSet<Integer> set = (HashSet<Integer>) IntStream.range(0, 100_000).boxed()
        .collect(Collectors.toSet());
    ArrayList<Integer> list = (ArrayList<Integer>) IntStream.range(0, 100_000).boxed()
        .collect(Collectors.toList());
    list.remove(0); // this reduces time 65x times

    set.removeAll(list);
  }
}
