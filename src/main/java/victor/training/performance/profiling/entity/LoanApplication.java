package victor.training.performance.profiling.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Data // periculos: toStringul generat de Lombok include si colectille pe care JPA le lazy-loadeaza cand faci log.trace("De tot rasu: " + entity);
//Solutii:
//1) @Getter @Setter ⭐️
//2) @ToString.Exclude pe colectii
@Entity
public class LoanApplication {
  public enum Status {NOT_STARTED, PENDING, APPROVED, DECLINED}

  @Id
  private Long id;
  private String title;
  @ElementCollection// #1 NU O FACE (fetch = FetchType.EAGER) // Evita lazy-loadul. dar urat: oriunde vreodata vei lua vreun LoanApplication de la Hibernate -> +1 SELECT / +1 JOIN
  private List<ApprovalStep> steps = new ArrayList<>();
  @ManyToMany
  private List<LoanClient> beneficiaries = new ArrayList<>();

  public Status getCurrentStatus() {
    return getLastStep().getStatus();
  }

  private ApprovalStep getLastStep() {
    List<ApprovalStep> startedSteps = steps.stream().filter(ApprovalStep::isStarted).collect(toList());
    if (startedSteps.isEmpty()) return steps.get(0);
    return startedSteps.get(startedSteps.size() - 1);
  }

  @Embeddable
  @Data
  public static class ApprovalStep {
    private String name;
    private Status status;

    boolean isStarted() {
      return status != Status.NOT_STARTED;
    }
  }

}
