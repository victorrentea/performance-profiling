package victor.training.performance.profiling.dto;

import lombok.Data;
import victor.training.performance.profiling.util.GDPRAspect.VisibleFor;

@Data
public class CommentDto {
  private String body;
  private String email;
}
