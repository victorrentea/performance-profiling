package victor.training.performance;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

public class CumPierdUnElementInHashMapSauSet {
  public static void main(String[] args) {
    Copil emma = new Copil();
    emma.setName("Emma");

    Set<Copil> copchii = new HashSet<>();
    copchii.add(emma);
    System.out.println(copchii.contains(emma)); // true
    System.out.println(copchii.size());

    emma.setName("Emma-Simona");
    System.out.println("pre-adolescenta: " + copchii.contains(emma)); // true
    copchii.add(emma);
    System.out.println(copchii.size());
  }
}


@Data
class Copil {
  private String name;
}