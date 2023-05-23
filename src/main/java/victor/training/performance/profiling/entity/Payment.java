package victor.training.performance.profiling.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
@Data
public class Payment {
  @Id
  private Long id;
  private LocalDate date;
  private Integer amount;

}
