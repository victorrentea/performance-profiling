package victor.training.performance.profiling;

import lombok.Data;
import victor.training.performance.profiling.util.GDPRAspect.VisibleFor;

@Data
public class CommentDto {
  private String body;
  @VisibleFor("ADMIN")
  private String email;
}
