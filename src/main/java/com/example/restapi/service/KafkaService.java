package com.example.restapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send a message to a Kafka topic
     * 
     * @param topic   The Kafka topic to send the message to
     * @param message The message to send
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<SendResult<String, String>> sendMessage(String topic, String message) {
        logger.info("Sending message to topic '{}': {}", topic, message);
        
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Message sent successfully to topic '{}' at partition {} with offset {}", 
                    topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to send message to topic '{}': {}", topic, ex.getMessage(), ex);
            }
        });
        
        return future;
    }

    /**
     * Send a message to a Kafka topic with a specific key
     * 
     * @param topic   The Kafka topic to send the message to
     * @param key     The message key
     * @param message The message to send
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<SendResult<String, String>> sendMessage(String topic, String key, String message) {
        logger.info("Sending message with key '{}' to topic '{}': {}", key, topic, message);
        
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Message with key '{}' sent successfully to topic '{}' at partition {} with offset {}", 
                    key, topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to send message with key '{}' to topic '{}': {}", key, topic, ex.getMessage(), ex);
            }
        });
        
        return future;
    }
} 