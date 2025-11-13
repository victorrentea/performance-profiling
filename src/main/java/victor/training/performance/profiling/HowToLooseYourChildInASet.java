package victor.training.performance.profiling;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

public class HowToLooseYourChildInASet {
  public static void main(String[] args) {
    Child poc = new Child().setName("Emma");
    Set<Child> set = new HashSet<>();
    set.add(poc);

    System.out.println(set.contains(poc)); // hashed{name:emma, id:null}
    // repo.save(poc);
    poc.setId(42L);

    System.out.println("WTF?!?--- because hash should include IMMUTABLE FIELDS");
    System.out.println(set.contains(poc)); // hashed{name:emma, id:42} != first hash
    set.add(poc);
    System.out.println(set.size());
  }
}
@Entity
@Data
class Child{
  @GeneratedValue
  @Id
  Long id;
  String name;
}