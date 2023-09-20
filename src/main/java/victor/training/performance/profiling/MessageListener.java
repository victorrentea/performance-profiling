package victor.training.performance.profiling;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {
  @KafkaListener(topics = "loanApproved", groupId = "profiled-app")
  public void handle(@Payload String payload) {
    System.out.println("Hello! + " + payload);
  }
}
