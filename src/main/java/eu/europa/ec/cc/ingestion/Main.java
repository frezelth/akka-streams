package eu.europa.ec.cc.ingestion;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage.Committable;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Consumer.DrainingControl;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.RunnableGraph;
import com.typesafe.config.Config;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class Main {

  final static ActorSystem system = ActorSystem.create("cc-ingestion-service");

  final static String bootstrapServers = system.settings().config().getString("bootstrapServers");

  final static ProducerSettings<String, byte[]> producerSettings =
      ProducerSettings.create(system.settings().config().getConfig("akka.kafka.producer"), new StringSerializer(), new ByteArraySerializer())
          .withBootstrapServers(bootstrapServers);

  final static CommitterSettings committerSettings = CommitterSettings.create(
      system.settings().config().getConfig("akka.kafka.committer"));

  final static org.apache.kafka.clients.producer.Producer<String, byte[]> kafkaProducer =
      producerSettings.createKafkaProducer();

  final static String consumerGroup = system.settings().config().getString("consumer.groupId");
  final static Config consumerConfig = system.settings().config().getConfig("akka.kafka.consumer");

  final static Properties properties = new Properties();

  static {
    properties.put(
        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers
    );
  }

  final static Admin admin = Admin.create(properties);

  private static void createTopicsIfNotExists(String providerId){
    NewTopic newTopic = new NewTopic("cc_dev_provider-" + providerId + "-event", 5,
        (short)1);
    admin.createTopics(Collections.singleton(newTopic));
  }

  private static RunnableGraph<DrainingControl<Done>> createProviderIngestion(
      String providerId,
      int messagesPerSecond
      ){

    createTopicsIfNotExists(providerId);

    ConsumerSettings<String, byte[]> consumerSettings =
        ConsumerSettings.create(consumerConfig, new StringDeserializer(), new ByteArrayDeserializer())
            .withBootstrapServers(bootstrapServers)
            .withGroupInstanceId(consumerGroup + "_" + providerId)
            .withGroupId(consumerGroup);

    return Consumer.committableSource(consumerSettings,
            Subscriptions.topics("cc_dev_provider-" + providerId + "-event"))
        .throttle(messagesPerSecond, Duration.ofSeconds(1))
        .map(
            msg ->
                ProducerMessage.<String, byte[], Committable>single(
                    new ProducerRecord<>("cc_dev_event", msg.record().key(), msg.record().value()),
                    msg.committableOffset()))
        .toMat(
            Producer.committableSink(producerSettings, committerSettings),
            Consumer::createDrainingControl);
  }

  public static void main(String[] argv) {

    Consumer.DrainingControl<Done> flexControl =
        createProviderIngestion("flex", 1).run(system);

    Consumer.DrainingControl<Done> camundaControl =
        createProviderIngestion("camunda", 2).run(system);

  }

}
