package victor.training.performance.profiling.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Payment {
  @Id
  private Long id;
  private LocalDate date;
  private Integer amount;

}
