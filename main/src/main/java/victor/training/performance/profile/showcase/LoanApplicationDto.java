package victor.training.performance.profile.showcase;

import lombok.Value;
import victor.training.performance.profile.showcase.LoanApplication.Status;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Value
public class LoanApplicationDto {
  Long id;
  String title;
  Status globalStatus;
  List<String> comments;

  public LoanApplicationDto(LoanApplication loanApplication, List<CommentDto> comments) {
    id = loanApplication.getId();
    title = loanApplication.getTitle();
    globalStatus = loanApplication.getCurrentStatus();
    this.comments = comments.stream().map(CommentDto::getBody).collect(toList());
  }
}
