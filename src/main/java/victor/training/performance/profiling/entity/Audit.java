package victor.training.performance.profiling.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
