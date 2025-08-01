package com.example.restapi.service;

import com.example.restapi.config.KafkaTopicConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Service
public class DynamicKafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicKafkaConsumerService.class);

    @Autowired
    private KafkaTopicConfig kafkaTopicConfig;

    @Autowired
    private InterestingEventService interestingEventService;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<ConcurrentMessageListenerContainer<String, String>> containers = new ArrayList<>();

    @PostConstruct
    public void initializeConsumers() {
        logger.info("Initializing dynamic Kafka consumers for {} topics", kafkaTopicConfig.getTopics().size());
        
        List<KafkaTopicConfig.TopicConfig> topics = kafkaTopicConfig.getTopics();
        for (KafkaTopicConfig.TopicConfig topicConfig : topics) {
            logger.info("Configuring consumer for topic: '{}' with correlated topic: '{}', consumer group: '{}', key of interest: '{}', correlated key of interest: '{}'", 
                topicConfig.getName(), topicConfig.getCorrelatedTopic(), topicConfig.getConsumerGroup(), 
                topicConfig.getKeyOfInterest(), topicConfig.getCorrelatedKeyOfInterest());
            
            // Create consumer for main topic
            createConsumerForTopic(topicConfig.getName(), topicConfig.getConsumerGroup(), topicConfig, false);
            
            // Create consumer for correlated topic
            createConsumerForTopic(topicConfig.getCorrelatedTopic(), topicConfig.getConsumerGroup() + "-correlated", topicConfig, true);
        }
        
        logger.info("Dynamic Kafka consumers initialized successfully. Total containers: {}", containers.size());
    }

    /**
     * Create a Kafka consumer container for a specific topic
     */
    private void createConsumerForTopic(String topic, String consumerGroup, KafkaTopicConfig.TopicConfig topicConfig, boolean isCorrelated) {
        try {
            // Create container properties
            ContainerProperties containerProperties = new ContainerProperties(topic);
            containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
            containerProperties.setGroupId(consumerGroup);

            // Create the container
            ConcurrentMessageListenerContainer<String, String> container = new ConcurrentMessageListenerContainer<>(
                consumerFactory, containerProperties);

            // Set the message listener
            container.setupMessageListener(new DynamicMessageListener(topicConfig, isCorrelated));

            // Start the container
            container.start();

            // Store the container for cleanup
            containers.add(container);

            logger.info("Created dynamic consumer for topic: '{}' with consumer group: '{}' (correlated: {})", 
                topic, consumerGroup, isCorrelated);

        } catch (Exception e) {
            logger.error("Error creating consumer for topic: '{}' with consumer group: '{}'", topic, consumerGroup, (Object) e);
        }
    }

    /**
     * Dynamic message listener implementation
     */
    private class DynamicMessageListener implements AcknowledgingMessageListener<String, String> {

        private final KafkaTopicConfig.TopicConfig topicConfig;
        private final boolean isCorrelated;

        public DynamicMessageListener(KafkaTopicConfig.TopicConfig topicConfig, boolean isCorrelated) {
            this.topicConfig = topicConfig;
            this.isCorrelated = isCorrelated;
        }

        @Override
        public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
            try {
                String topic = record.topic();
                String key = record.key();
                String value = record.value();
                int partition = record.partition();
                long offset = record.offset();
                String consumerGroup = isCorrelated ? topicConfig.getConsumerGroup() + "-correlated" : topicConfig.getConsumerGroup();
                
                logger.info("Consumer Group: '{}' | Topic: '{}' | Partition: {} | Offset: {} | Key: '{}' | Message: '{}'", 
                    consumerGroup, topic, partition, offset, key, value);
                
                // Extract and log keys of interest from JSON message
                extractAndLogKeysOfInterest(topic, value, consumerGroup);
                
                logger.debug("Message processed successfully for topic: {} with consumer group: {}", topic, consumerGroup);
                
                // Acknowledge the message after successful processing
                acknowledgment.acknowledge();
                logger.debug("Message acknowledged for topic: {} with consumer group: {}", topic, consumerGroup);
                
            } catch (Exception e) {
                logger.error("Error processing message for topic: {} (correlated: {})", record.topic(), isCorrelated, (Object) e);
                // In a real application, you might want to implement dead letter queue logic here
                // Note: We still acknowledge the message even on error to prevent infinite reprocessing
                // In a production environment, you might want to implement retry logic or dead letter queue
                try {
                    acknowledgment.acknowledge();
                    logger.debug("Message acknowledged after error for topic: {} with consumer group: {}", record.topic(), 
                        isCorrelated ? topicConfig.getConsumerGroup() + "-correlated" : topicConfig.getConsumerGroup());
                } catch (Exception ackException) {
                    logger.error("Error acknowledging message after processing error", (Object) ackException);
                }
            }
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
                            topic, keyName, keyOfInterest, (Object) e);
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
                            originalKeyName, keyOfInterest, topic, (Object) e);
                    }
                } else if (keyName != null) {
                    logger.warn("Consumer Group: '{}' | Topic: '{}' | Correlated Key of Interest '{}' not found in JSON message", 
                        consumerGroup, topic, keyName);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error extracting keys of interest from JSON message for topic: {} and consumer group: {}", 
                topic, consumerGroup, (Object) e);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Stopping {} dynamic Kafka consumer containers", containers.size());
        for (ConcurrentMessageListenerContainer<String, String> container : containers) {
            try {
                container.stop();
                logger.debug("Stopped container for topic: {}", container.getContainerProperties().getTopics());
            } catch (Exception e) {
                logger.error("Error stopping container", (Object) e);
            }
        }
        containers.clear();
        logger.info("All dynamic Kafka consumer containers stopped");
    }
} 