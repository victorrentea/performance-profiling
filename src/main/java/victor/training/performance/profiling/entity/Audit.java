package victor.training.performance.profiling.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Audit {
  @Id
  @GeneratedValue
  private Long id;
  private String text;

  public Audit(String text) {
    this.text = text;
  }
}
