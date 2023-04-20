package victor.training.performance.profiling;

import java.util.HashSet;
import java.util.Objects;

public class LetsBreakHashMap {
  public static void main(String[] args) {
    HashSet<Key> set = new HashSet<>();
    set.add(new Key("Victor", "123"));
    System.out.println(set.contains(new Key("Victor", "123")));
  }
}
class Key {
  private final String name;
  private final String phone;

  Key(String name, String phone) {
    this.name = name;
    this.phone = phone;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Key key = (Key) o;
    return Objects.equals(name, key.name) && Objects.equals(phone, key.phone);
  }
  // without this hashCode, i cannot find my element in the set
  @Override
  public int hashCode() {
//    return Objects.hash(name, phone);
    return 1; //this will trash the performance of a hashset/map
  }
  // we are breaking the hashcode/equals contract.
  // a.equals(b) => a.hashCode() == b.hashCode()


}
