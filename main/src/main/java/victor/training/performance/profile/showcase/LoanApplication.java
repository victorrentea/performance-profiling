package victor.training.performance.profile.showcase;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Data
@Entity
public class LoanApplication {
  enum Status {NOT_STARTED, PENDING, APPROVED, DECLINED}

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
