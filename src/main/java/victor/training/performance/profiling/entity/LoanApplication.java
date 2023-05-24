package victor.training.performance.profiling.entity;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Data
@Entity
public class LoanApplication {
  public enum Status {NOT_STARTED, PENDING, APPROVED, DECLINED}

  @Id
  private Long id;
  private String title;
  @ElementCollection
  @ToString.Exclude
  private List<ApprovalStep> steps = new ArrayList<>();
  @ManyToMany
  @ToString.Exclude
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
