package org.example;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

public class SimpleConsumer {

    private final static Logger logger = LoggerFactory.getLogger(SimpleProducer.class);
    private final static String TOPIC_NAME = "test";
    private final static String BOOTSTRAP_SERVERS = "my-kafka:9092";
    private final static String GROUP_ID = "test-group";

    private static KafkaConsumer<String, String> consumer;
    private static Map<TopicPartition, OffsetAndMetadata> currentOffset;

    public static void main(String[] args) {
        Properties configs = new Properties();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        consumer = new KafkaConsumer<>(configs);
        consumer.subscribe(Arrays.asList(TOPIC_NAME), new RebalanceListener());

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                currentOffset = new HashMap<>();
                for (ConsumerRecord<String, String> record : records) {
                    logger.info("{}", record);
                    currentOffset.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1, null) // 현재 오프셋에 1을 더한 값으로 커밋해야한다. poll()메서드는 마지막으로 커밋된 오프셋부터 레코드를 리턴하기 때문이다.
                    );
                    consumer.commitSync(currentOffset);
                }
            }
        } catch (WakeupException e) {
            logger.warn("Wake up consumer");
        } finally {
            consumer.close();
            Runtime.getRuntime().addShutdownHook(new ShutdownThread()); // 셧다운훅 발생
        }

    }

    static class ShutdownThread extends Thread {
        public void run() {
            logger.info("Shutdown hook");
            consumer.wakeup();
        }
    }

    private static class RebalanceListener implements ConsumerRebalanceListener {
        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            logger.warn("Partitions are assigned");
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            logger.warn("Partitions are assigned");
            consumer.commitSync(currentOffset);
        }
    }
}
