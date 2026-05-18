package victor.training.performance.profiling.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import victor.training.performance.profiling.entity.Loan.ApprovalStep.Status;

import java.util.ArrayList;
import java.util.List;

@Entity
//@Data // generates @HashCodeEquals and @ToString on ALL FIELDS
@Getter
@Setter
public class Loan {
  @Id
  private Long id;
  private String title;
  @ElementCollection
  private List<ApprovalStep> steps = new ArrayList<>();
  @ManyToMany
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
    public enum Status {NOT_STARTED, PENDING, APPROVED, DECLINED}
    private Status status;

    boolean isStarted() {
      return status != Status.NOT_STARTED;
    }
  }

}
