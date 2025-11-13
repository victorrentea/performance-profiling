package victor.training.performance.profiling.dto;

import lombok.Value;
import victor.training.performance.profiling.entity.Loan;
import victor.training.performance.profiling.entity.Loan.ApprovalStep.Status;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Value
public class LoanDto {
  Long id;
  String title;
  Status globalStatus;
  List<String> comments;

  public LoanDto(Loan loan, List<CommentDto> comments) {
    id = loan.getId();
    title = loan.getTitle();
    globalStatus = loan.getCurrentStatus(); // uses .steps
    this.comments = comments.stream().map(CommentDto::body).collect(toList());
  }
}
