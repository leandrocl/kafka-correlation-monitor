package com.example.restapi.integration;

import com.example.restapi.entity.InterestingEvent;
import com.example.restapi.repository.InterestingEventRepository;
import com.example.restapi.service.InterestingEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Kafka Testcontainers Integration Tests")
class KafkaTestcontainersIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private InterestingEventRepository interestingEventRepository;

    @Autowired
    private InterestingEventService interestingEventService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;
    private KafkaProducer<String, String> producer;
    private KafkaConsumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        interestingEventRepository.deleteAll();
        
        // Setup Kafka producer
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        producer = new KafkaProducer<>(producerProps);

        // Setup Kafka consumer
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-manual-consumer-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumer = new KafkaConsumer<>(consumerProps);
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.close();
        }
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("Should produce and consume messages from Kafka using Testcontainers")
    void shouldProduceAndConsumeMessagesFromKafka() throws Exception {
        // Given
        String topicName = "manual-test-topic";
        String key = "test-key";
        String message = "{\"testId\":\"test123\",\"message\":\"Test message from Testcontainers\"}";

        // When - Produce message to Kafka
        ProducerRecord<String, String> record = new ProducerRecord<>(topicName, key, message);
        producer.send(record).get();

        // Then - Consume message from Kafka
        consumer.subscribe(Collections.singletonList(topicName));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        
        assertFalse(records.isEmpty(), "Should have received at least one message");
        
        ConsumerRecord<String, String> consumedRecord = records.iterator().next();
        assertEquals(key, consumedRecord.key());
        assertEquals(message, consumedRecord.value());
        assertEquals(topicName, consumedRecord.topic());
    }

    @Test
    @DisplayName("Should process interesting events from Kafka messages")
    void shouldProcessInterestingEventsFromKafkaMessages() throws Exception {
        // Given
        String topicName = "test-topic";
        String key = "user123";
        String message = "{\"userId\":\"user123\",\"message\":\"Test message for correlation\"}";

        // When - Produce message to Kafka
        ProducerRecord<String, String> record = new ProducerRecord<>(topicName, key, message);
        producer.send(record).get();

        // Wait a bit for processing
        Thread.sleep(3000);

        // Then - Verify interesting event was created
        List<InterestingEvent> events = interestingEventRepository.findAll();
        assertFalse(events.isEmpty(), "Should have created at least one interesting event");
        
        InterestingEvent event = events.get(0);
        assertEquals(topicName, event.getTopicName());
        assertEquals("userId", event.getKeyOfInterestName());
        assertEquals("user123", event.getKeyOfInterestValue());
        assertFalse(event.getIsCorrelated());
    }

    @Test
    @DisplayName("Should handle correlation between main and correlated topics")
    void shouldHandleCorrelationBetweenMainAndCorrelatedTopics() throws Exception {
        // Given
        String mainTopic = "test-topic";
        String correlatedTopic = "test-topic-correlated";
        String userId = "user123";
        String correlationId = "user123";

        // When - Send message to main topic
        String mainMessage = "{\"userId\":\"" + userId + "\",\"message\":\"Main message\"}";
        ProducerRecord<String, String> mainRecord = new ProducerRecord<>(mainTopic, userId, mainMessage);
        producer.send(mainRecord).get();

        // Wait for processing
        Thread.sleep(3000);

        // Send correlated message
        String correlatedMessage = "{\"correlationId\":\"" + correlationId + "\",\"message\":\"Correlated message\"}";
        ProducerRecord<String, String> correlatedRecord = new ProducerRecord<>(correlatedTopic, correlationId, correlatedMessage);
        producer.send(correlatedRecord).get();

        // Wait for processing
        Thread.sleep(3000);

        // Then - Verify correlation
        List<InterestingEvent> events = interestingEventRepository.findAll();
        assertFalse(events.isEmpty(), "Should have created interesting events");
        
        // Find the correlated event
        InterestingEvent correlatedEvent = events.stream()
                .filter(event -> event.getIsCorrelated())
                .findFirst()
                .orElse(null);
        
        assertNotNull(correlatedEvent, "Should have a correlated event");
        assertEquals(mainTopic, correlatedEvent.getTopicName());
        assertEquals("userId", correlatedEvent.getKeyOfInterestName());
        assertEquals(userId, correlatedEvent.getKeyOfInterestValue());
        assertTrue(correlatedEvent.getIsCorrelated());
        assertNotNull(correlatedEvent.getCorrelatedMessage());
        assertNotNull(correlatedEvent.getCorrelationTimestamp());
    }
}
