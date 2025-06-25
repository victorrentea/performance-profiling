package victor.training.performance.profiling.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
// generates @HashCodeEquals and @ToString on ALL FIELDS
//@Getter @Setter
public class LoanApplication {
  public LoanApplication() {
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public List<ApprovalStep> getSteps() {
    return steps;
  }

  public List<LoanClient> getBeneficiaries() {
    return beneficiaries;
  }

  public LoanApplication setId(Long id) {
    this.id = id;
    return this;
  }

  public LoanApplication setTitle(String title) {
    this.title = title;
    return this;
  }

  public LoanApplication setSteps(List<ApprovalStep> steps) {
    this.steps = steps;
    return this;
  }

  public LoanApplication setBeneficiaries(List<LoanClient> beneficiaries) {
    this.beneficiaries = beneficiaries;
    return this;
  }

  public boolean equals(final Object o) {
    if (o == this) return true;
    if (!(o instanceof LoanApplication)) return false;
    final LoanApplication other = (LoanApplication) o;
    if (!other.canEqual((Object) this)) return false;
    final Object this$id = id;
    final Object other$id = other.id;
    if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
    final Object this$title = title;
    final Object other$title = other.title;
    if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
    final Object this$steps = steps;
    final Object other$steps = other.steps;
    if (this$steps == null ? other$steps != null : !this$steps.equals(other$steps)) return false;
    final Object this$beneficiaries = beneficiaries;
    final Object other$beneficiaries = other.beneficiaries;
    if (this$beneficiaries == null ? other$beneficiaries != null : !this$beneficiaries.equals(other$beneficiaries))
      return false;
    return true;
  }

  protected boolean canEqual(final Object other) {
    return other instanceof LoanApplication;
  }

  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final Object $id = id;
    result = result * PRIME + ($id == null ? 43 : $id.hashCode());
    final Object $title = title;
    result = result * PRIME + ($title == null ? 43 : $title.hashCode());
    final Object $steps = steps;
    result = result * PRIME + ($steps == null ? 43 : $steps.hashCode());
    final Object $beneficiaries = beneficiaries;
    result = result * PRIME + ($beneficiaries == null ? 43 : $beneficiaries.hashCode());
    return result;
  }



  public enum Status {NOT_STARTED, PENDING, APPROVED, DECLINED}

  @Id
  private Long id;
  private String title;
  @ElementCollection
  private List<ApprovalStep> steps = new ArrayList<>();
  @ManyToMany
  private List<LoanClient> beneficiaries = new ArrayList<>();

  public String toString() {
    return "LoanApplication(id=" + id +
           ", title=" + title +
           ", steps=" + steps +
           ", beneficiaries=" + beneficiaries + ")";
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
