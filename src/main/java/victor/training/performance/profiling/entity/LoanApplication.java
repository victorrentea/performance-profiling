package victor.training.performance.profiling.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@Data// lombok genereaza: getteri, setteri, constructori, equals, hashCode, toString
// drama e ca in toStringul generat au fost incluse si cele 2 colectii
// dar cele 2 colectii sunt LAZY, deci sunt aduse (SELECT in DB) la nevoie, la primul access
@Entity
public class LoanApplication {
  public enum Status {NOT_STARTED, PENDING, APPROVED, DECLINED}

  @Id
  private Long id;
  private String title;
  @ElementCollection
  @ToString.Exclude // sa nu cauzez lazy load la toString
  private List<ApprovalStep> steps = new ArrayList<>();
  @ManyToMany
  @ToString.Exclude // sa nu cauzez lazy load la toString
  private List<LoanClient> beneficiaries = new ArrayList<>();

  public Status getCurrentStatus() {
    return getLastStep().getStatus();
  }

  private ApprovalStep getLastStep() {
    List<ApprovalStep> startedSteps = steps.stream()
        .filter(ApprovalStep::isStarted)
        .toList();
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
