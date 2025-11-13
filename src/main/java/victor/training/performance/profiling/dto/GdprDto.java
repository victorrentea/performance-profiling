package victor.training.performance.profiling.dto;

import lombok.Data;
import victor.training.performance.profiling.util.GDPRAspect.VisibleFor;

@Data
public class GdprDto {
  String feedback;
  @VisibleFor("ADMIN")
  String authorName; // = null if user is NOT ADMIN
  @VisibleFor("ADMIN")
  String authorEmail;
}
