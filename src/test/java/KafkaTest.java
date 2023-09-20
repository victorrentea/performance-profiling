import base.GatlingEngine;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import ru.tinkoff.gatling.kafka.javaapi.protocol.KafkaProtocolBuilder;

import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.time.Duration.ofSeconds;
import static ru.tinkoff.gatling.kafka.javaapi.KafkaDsl.kafka;

public class KafkaTest extends Simulation {
  public static void main(String[] args) {
    GatlingEngine.startClass(KafkaTest.class);
  }

  private final KafkaProtocolBuilder kafkaProtocol = kafka()
      .topic("loanApproved")
      .properties(
          Map.of(
              ProducerConfig.ACKS_CONFIG, "1",
              ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
              ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer",
              ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG , "org.apache.kafka.common.serialization.StringSerializer")
      );

//  private final Headers headers = new RecordHeaders(new Header[]{new RecordHeader("test-header", "value".getBytes())});
  private final ScenarioBuilder kafkaProducer = scenario("Kafka Producer")
      .exec(kafka("Simple Message")
          .send("key","value" /*headers*/)
      );

  {
    setUp(
        kafkaProducer.injectOpen(incrementUsersPerSec(1000)
            .times(4).eachLevelLasting(60)
            .separatedByRampsLasting(10)
            .startingFrom(100.0))
    ).protocols(kafkaProtocol);
  }
}
