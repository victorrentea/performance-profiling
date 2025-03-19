package victor.training.performance.profiling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class MemoryChurn {
  public static void main(String[] args) {
    List<String> list = IntStream.range(0, 10000)
        .mapToObj(i -> "x".repeat(20))
        .toList();
    R r = new R(list);
    for (int i = 0; i < 100000; i++) {
      System.out.println(r.getList().size());
    }
  }
}
record R(List<String> list) {
  List<String> getList() {
    return Collections.unmodifiableList(list); // GOOD
  }
}
