package victor.training.performance.profiling;

import lombok.Data;

import javax.persistence.Id;
import java.util.HashSet;

public class HowToLooseAnEntry {
  public static void main(String[] args) {

    HashSet<Child> set = new HashSet<>();

    Child c = new Child();
    c.setName("Emma");
    System.out.println(c.hashCode());
    set.add(c);


    c.setName("Emma-Simona");
    System.out.println(c.hashCode());
    set.add(c);

    System.out.println("How many children do I have ?!");
    System.out.println(set.size());
    System.out.println(set);

  }

  @Data
  private static class Child {
  //  @Id
  //  private Long id;
    String name;
  }
}


// hash/equals should only involve immutable fields.
// whenever two objects are equals they should have the same hashCode
