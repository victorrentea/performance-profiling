package victor.training.performance.profiling.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Entity
// ❌ NEVER USE @Data on @Entity!!!!
//@Data // (lombok) generates @HashCodeEquals and @ToString on ALL FIELDS :
// instead use just
@Getter @Setter
public class LoanApplication { // toString
  public enum Status {NOT_STARTED, PENDING, APPROVED, DECLINED}

  @Id
  private Long id;
  private String title;
  @ElementCollection
  private List<ApprovalStep> steps = new ArrayList<>();
  @ManyToMany
  private List<LoanClient> beneficiaries = new ArrayList<>();

  @Override
  public String toString() {
    return "LoanApplication{" +
           "id=" + id +
           ", title='" + title + '\'' +
           '}';
  }

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
