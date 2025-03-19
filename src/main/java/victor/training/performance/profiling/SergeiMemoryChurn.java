package victor.training.performance.profiling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class SergeiMemoryChurn {
  public static void main(String[] args) {
    List<String> list = IntStream.range(0, 10000).mapToObj(i -> "x".repeat(1000)).toList();

    R r = new R(list);

    for (int i = 0; i < 100; i++) {
      System.out.println(r.getList().size());
//      List<String> list1 = r.getList();
//      list1.add("x");
    }

  }
}

record R(List<String> list) {
  List<String> getList() {
    return new ArrayList<>(list); // BAD: clones the coll
//    return Collections.unmodifiableList(list); // better allocates few bytes
  }
}
