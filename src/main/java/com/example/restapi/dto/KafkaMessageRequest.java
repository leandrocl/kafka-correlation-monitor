package com.example.restapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for Kafka message requests
 */
public class KafkaMessageRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String message;

    @NotBlank(message = "Kafka topic cannot be empty")
    @Size(max = 100, message = "Topic name cannot exceed 100 characters")
    private String kafkaTopic;

    // Default constructor
    public KafkaMessageRequest() {}

    // Constructor with parameters
    public KafkaMessageRequest(String message, String kafkaTopic) {
        this.message = message;
        this.kafkaTopic = kafkaTopic;
    }

    // Getters and setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public void setKafkaTopic(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
    }

    @Override
    public String toString() {
        return "KafkaMessageRequest{" +
                "message='" + message + '\'' +
                ", kafkaTopic='" + kafkaTopic + '\'' +
                '}';
    }
} 