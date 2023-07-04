package eu.europa.ec.cc.ingestion;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class ProducerTest {


  public static void main(String[] argv) {
    String bootstrapServers = "127.0.0.1:9092";

    // create Producer properties
    Properties properties = new Properties();
    properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

    // create the producer
    KafkaProducer<String, byte[]> producer = new KafkaProducer<>(properties);

    // create a producer record
    for (int i=0;i<100;i++){
      ProducerRecord<String, byte[]> producerRecord =
          new ProducerRecord<>("cc_dev_provider-flex-event", "hello flex".getBytes());

      // send data
      producer.send(producerRecord);

      producerRecord =
          new ProducerRecord<>("cc_dev_provider-camunda-event", "hello camunda".getBytes());
      producer.send(producerRecord);
    }

    // flush data - synchronous
    producer.flush();
    // flush and close producer
    producer.close();
  }

}
