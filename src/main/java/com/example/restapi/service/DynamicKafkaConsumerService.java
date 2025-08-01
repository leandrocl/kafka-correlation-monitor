package com.example.restapi.service;

import com.example.restapi.config.KafkaTopicConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Service
public class DynamicKafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicKafkaConsumerService.class);

    @Autowired
    private KafkaTopicConfig kafkaTopicConfig;

    @Autowired
    private InterestingEventService interestingEventService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initializeConsumers() {
        logger.info("Initializing dynamic Kafka consumers for {} topics", kafkaTopicConfig.getTopics().size());
        
        List<KafkaTopicConfig.TopicConfig> topics = kafkaTopicConfig.getTopics();
        for (KafkaTopicConfig.TopicConfig topicConfig : topics) {
            logger.info("Configured consumer for topic: '{}' with correlated topic: '{}', consumer group: '{}', key of interest: '{}', correlated key of interest: '{}'", 
                topicConfig.getName(), topicConfig.getCorrelatedTopic(), topicConfig.getConsumerGroup(), 
                topicConfig.getKeyOfInterest(), topicConfig.getCorrelatedKeyOfInterest());
        }
        
        logger.info("Dynamic Kafka consumers configuration loaded successfully");
    }

    /**
     * Consumer for test-topic (main topic)
     */
    @KafkaListener(
        topics = "test-topic",
        groupId = "test-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTestTopic(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processMessage(record, ack, "test-consumer-group");
    }

    /**
     * Consumer for test-topic-correlated (correlated topic)
     */
    @KafkaListener(
        topics = "test-topic-correlated",
        groupId = "test-consumer-group-correlated",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTestTopicCorrelated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processMessage(record, ack, "test-consumer-group-correlated");
    }

    /**
     * Consumer for test-topic-json (main topic)
     */
    @KafkaListener(
        topics = "test-topic-json",
        groupId = "json-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTestTopicJson(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processMessage(record, ack, "json-consumer-group");
    }

    /**
     * Consumer for test-topic-json-correlated (correlated topic)
     */
    @KafkaListener(
        topics = "test-topic-json-correlated",
        groupId = "json-consumer-group-correlated",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTestTopicJsonCorrelated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processMessage(record, ack, "json-consumer-group-correlated");
    }

    /**
     * Consumer for user-events (main topic)
     */
    @KafkaListener(
        topics = "user-events",
        groupId = "user-events-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserEvents(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processMessage(record, ack, "user-events-consumer-group");
    }

    /**
     * Consumer for user-events-correlated (correlated topic)
     */
    @KafkaListener(
        topics = "user-events-correlated",
        groupId = "user-events-consumer-group-correlated",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserEventsCorrelated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processMessage(record, ack, "user-events-consumer-group-correlated");
    }

    /**
     * Consumer for system-events (main topic)
     */
    @KafkaListener(
        topics = "system-events",
        groupId = "system-events-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSystemEvents(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processMessage(record, ack, "system-events-consumer-group");
    }

    /**
     * Consumer for system-events-correlated (correlated topic)
     */
    @KafkaListener(
        topics = "system-events-correlated",
        groupId = "system-events-consumer-group-correlated",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSystemEventsCorrelated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processMessage(record, ack, "system-events-consumer-group-correlated");
    }

    /**
     * Process a consumed message
     */
    private void processMessage(ConsumerRecord<String, String> record, Acknowledgment ack, String consumerGroup) {
        try {
            String topic = record.topic();
            String key = record.key();
            String value = record.value();
            int partition = record.partition();
            long offset = record.offset();
            
            logger.info("Consumer Group: '{}' | Topic: '{}' | Partition: {} | Offset: {} | Key: '{}' | Message: '{}'", 
                consumerGroup, topic, partition, offset, key, value);
            
            // Extract and log keys of interest from JSON message
            extractAndLogKeysOfInterest(topic, value, consumerGroup);
            
            // Acknowledge the message
            ack.acknowledge();
            
            logger.debug("Message acknowledged successfully for topic: {} with consumer group: {}", topic, consumerGroup);
            
        } catch (Exception e) {
            logger.error("Error processing message for consumer group: {}", consumerGroup, e);
            // In a real application, you might want to implement dead letter queue logic here
        }
    }

    /**
     * Extract and log keys of interest from JSON message
     */
    private void extractAndLogKeysOfInterest(String topic, String message, String consumerGroup) {
        try {
            // Find the topic configuration
            KafkaTopicConfig.TopicConfig topicConfig = kafkaTopicConfig.getTopics().stream()
                .filter(config -> config.getName().equals(topic) || config.getCorrelatedTopic().equals(topic))
                .findFirst()
                .orElse(null);

            if (topicConfig == null) {
                logger.warn("No topic configuration found for topic: {}", topic);
                return;
            }

            // Parse JSON message
            JsonNode jsonNode = objectMapper.readTree(message);
            
            // Determine which key to extract based on whether this is the main topic or correlated topic
            String keyOfInterest = null;
            String keyName = null;
            
            if (topic.equals(topicConfig.getName())) {
                // Main topic
                keyName = topicConfig.getKeyOfInterest();
                if (keyName != null && jsonNode.has(keyName)) {
                    keyOfInterest = jsonNode.get(keyName).asText();
                    logger.info("Consumer Group: '{}' | Topic: '{}' | Key of Interest '{}': '{}'", 
                        consumerGroup, topic, keyName, keyOfInterest);
                    
                    // Persist interesting event to database
                    try {
                        interestingEventService.saveInterestingEvent(topic, keyName, keyOfInterest);
                        logger.info("Persisted interesting event to database - Topic: '{}', Key: '{}', Value: '{}'", 
                            topic, keyName, keyOfInterest);
                    } catch (Exception e) {
                        logger.error("Error persisting interesting event to database - Topic: '{}', Key: '{}', Value: '{}'", 
                            topic, keyName, keyOfInterest, e);
                    }
                } else if (keyName != null) {
                    logger.warn("Consumer Group: '{}' | Topic: '{}' | Key of Interest '{}' not found in JSON message", 
                        consumerGroup, topic, keyName);
                }
            } else if (topic.equals(topicConfig.getCorrelatedTopic())) {
                // Correlated topic
                keyName = topicConfig.getCorrelatedKeyOfInterest();
                if (keyName != null && jsonNode.has(keyName)) {
                    keyOfInterest = jsonNode.get(keyName).asText();
                    logger.info("Consumer Group: '{}' | Topic: '{}' | Correlated Key of Interest '{}': '{}'", 
                        consumerGroup, topic, keyName, keyOfInterest);
                    
                    // Check for correlation with existing interesting events
                    // We need to look for the original key name, not the correlated key name
                    String originalKeyName = topicConfig.getKeyOfInterest();
                    try {
                        interestingEventService.checkAndUpdateCorrelation(originalKeyName, keyOfInterest, message);
                        logger.info("Checked correlation for original key: '{}' with value: '{}' from correlated topic: '{}'", 
                            originalKeyName, keyOfInterest, topic);
                    } catch (Exception e) {
                        logger.error("Error checking correlation for original key: '{}' with value: '{}' from correlated topic: '{}'", 
                            originalKeyName, keyOfInterest, topic, e);
                    }
                } else if (keyName != null) {
                    logger.warn("Consumer Group: '{}' | Topic: '{}' | Correlated Key of Interest '{}' not found in JSON message", 
                        consumerGroup, topic, keyName);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error extracting keys of interest from JSON message for topic: {} and consumer group: {}", 
                topic, consumerGroup, e);
        }
    }
} 