package victor.training.performance.profiling.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class LoanClient {
  @Id
  @GeneratedValue
  private Long id;
  private String name;
  private LocalDate birthDate;
  private String occupation;
}
