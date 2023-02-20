package victor.training.performance.profile.showcase;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
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
