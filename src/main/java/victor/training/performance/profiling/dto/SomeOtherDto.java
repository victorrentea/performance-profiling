package victor.training.performance.profiling.dto;

import lombok.Data;
import victor.training.performance.profiling.util.GDPRAspect.VisibleFor;

@Data
public class SomeOtherDto {
  String feedback;
  String authorName;
  @VisibleFor("ADMIN")
  String authorEmail; // set to null for users not ADMINs
}
