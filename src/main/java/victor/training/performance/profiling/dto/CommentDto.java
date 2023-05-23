package victor.training.performance.profiling.dto;

import lombok.Data;
import lombok.Value;
import victor.training.performance.profiling.util.GDPRAspect.VisibleFor;

@Value
public class CommentDto {
  String body;
  String email;
}
