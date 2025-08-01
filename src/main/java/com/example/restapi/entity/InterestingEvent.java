package com.example.restapi.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interesting_events")
public class InterestingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_name", nullable = false, length = 100)
    private String topicName;

    @Column(name = "key_of_interest_name", nullable = false, length = 100)
    private String keyOfInterestName;

    @Column(name = "key_of_interest_value", nullable = false, length = 500)
    private String keyOfInterestValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "correlated_message", length = 1000)
    private String correlatedMessage;

    @Column(name = "is_correlated", nullable = false)
    private Boolean isCorrelated = false;

    @Column(name = "correlation_timestamp")
    private Instant correlationTimestamp;

    // Default constructor
    public InterestingEvent() {
        this.createdAt = Instant.now(); // UTC timestamp
        this.isCorrelated = false;
    }

    // Constructor with parameters
    public InterestingEvent(String topicName, String keyOfInterestName, String keyOfInterestValue) {
        this.topicName = topicName;
        this.keyOfInterestName = keyOfInterestName;
        this.keyOfInterestValue = keyOfInterestValue;
        this.createdAt = Instant.now(); // UTC timestamp
        this.isCorrelated = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getKeyOfInterestName() {
        return keyOfInterestName;
    }

    public void setKeyOfInterestName(String keyOfInterestName) {
        this.keyOfInterestName = keyOfInterestName;
    }

    public String getKeyOfInterestValue() {
        return keyOfInterestValue;
    }

    public void setKeyOfInterestValue(String keyOfInterestValue) {
        this.keyOfInterestValue = keyOfInterestValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCorrelatedMessage() {
        return correlatedMessage;
    }

    public void setCorrelatedMessage(String correlatedMessage) {
        this.correlatedMessage = correlatedMessage;
    }

    public Boolean getIsCorrelated() {
        return isCorrelated;
    }

    public void setIsCorrelated(Boolean isCorrelated) {
        this.isCorrelated = isCorrelated;
    }

    public Instant getCorrelationTimestamp() {
        return correlationTimestamp;
    }

    public void setCorrelationTimestamp(Instant correlationTimestamp) {
        this.correlationTimestamp = correlationTimestamp;
    }

    @Override
    public String toString() {
        return "InterestingEvent{" +
                "id=" + id +
                ", topicName='" + topicName + '\'' +
                ", keyOfInterestName='" + keyOfInterestName + '\'' +
                ", keyOfInterestValue='" + keyOfInterestValue + '\'' +
                ", createdAt=" + createdAt +
                ", correlatedMessage='" + correlatedMessage + '\'' +
                ", isCorrelated=" + isCorrelated +
                ", correlationTimestamp=" + correlationTimestamp +
                '}';
    }
} 