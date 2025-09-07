package com.example.restapi.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaService Unit Tests")
class KafkaServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaService kafkaService;

    private CompletableFuture<SendResult<String, String>> future;

    @BeforeEach
    void setUp() {
        // No setup needed for CompletableFuture
    }

    @Test
    @DisplayName("Should send message to Kafka topic successfully")
    void shouldSendMessageToKafkaTopicSuccessfully() throws ExecutionException, InterruptedException {
        // Given
        String topic = "test-topic";
        String message = "{\"userId\":\"user123\",\"message\":\"Test message\"}";
        
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, message);
        RecordMetadata recordMetadata = new RecordMetadata(
            new TopicPartition(topic, 0), 1L, 0L, 0L, 0L, 0, 0
        );
        SendResult<String, String> sendResult = new SendResult<>(producerRecord, recordMetadata);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq(topic), eq(message)))
            .thenReturn(future);

        // When
        CompletableFuture<SendResult<String, String>> result = kafkaService.sendMessage(topic, message);

        // Then
        assertNotNull(result);
        SendResult<String, String> actualResult = result.get();
        assertEquals(sendResult, actualResult);
        verify(kafkaTemplate, times(1)).send(topic, message);
    }

    @Test
    @DisplayName("Should handle Kafka send failure")
    void shouldHandleKafkaSendFailure() {
        // Given
        String topic = "test-topic";
        String message = "{\"userId\":\"user123\",\"message\":\"Test message\"}";
        RuntimeException exception = new RuntimeException("Kafka send failed");
        
        CompletableFuture<SendResult<String, String>> failedFuture = CompletableFuture.failedFuture(exception);
        
        when(kafkaTemplate.send(eq(topic), eq(message)))
            .thenReturn(failedFuture);

        // When & Then
        CompletableFuture<SendResult<String, String>> result = kafkaService.sendMessage(topic, message);
        
        assertThrows(ExecutionException.class, () -> {
            result.get();
        });
        
        verify(kafkaTemplate, times(1)).send(topic, message);
    }

    @Test
    @DisplayName("Should send message with key successfully")
    void shouldSendMessageWithKeySuccessfully() throws ExecutionException, InterruptedException {
        // Given
        String topic = "test-topic";
        String key = "user123";
        String message = "{\"userId\":\"user123\",\"message\":\"Test message\"}";
        
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, message);
        RecordMetadata recordMetadata = new RecordMetadata(
            new TopicPartition(topic, 0), 1L, 0L, 0L, 0L, 0, 0
        );
        SendResult<String, String> sendResult = new SendResult<>(producerRecord, recordMetadata);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq(topic), eq(key), eq(message)))
            .thenReturn(future);

        // When
        CompletableFuture<SendResult<String, String>> result = kafkaService.sendMessage(topic, key, message);

        // Then
        assertNotNull(result);
        SendResult<String, String> actualResult = result.get();
        assertEquals(sendResult, actualResult);
        verify(kafkaTemplate, times(1)).send(topic, key, message);
    }

    @Test
    @DisplayName("Should handle null message gracefully")
    void shouldHandleNullMessageGracefully() {
        // Given
        String topic = "test-topic";
        String message = null;

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            kafkaService.sendMessage(topic, message);
        });
        
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle empty topic gracefully")
    void shouldHandleEmptyTopicGracefully() throws ExecutionException, InterruptedException {
        // Given
        String topic = "";
        String message = "{\"userId\":\"user123\",\"message\":\"Test message\"}";
        
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, message);
        RecordMetadata recordMetadata = new RecordMetadata(
            new TopicPartition(topic, 0), 1L, 0L, 0L, 0L, 0, 0
        );
        SendResult<String, String> sendResult = new SendResult<>(producerRecord, recordMetadata);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq(topic), eq(message)))
            .thenReturn(future);

        // When & Then - Empty string is valid for KafkaTemplate, so it should succeed
        CompletableFuture<SendResult<String, String>> result = kafkaService.sendMessage(topic, message);
        
        // Verify the result
        SendResult<String, String> actualResult = result.get();
        assertEquals(sendResult, actualResult);
        
        // Verify that the service was called
        verify(kafkaTemplate, times(1)).send(topic, message);
    }

    @Test
    @DisplayName("Should handle null topic gracefully")
    void shouldHandleNullTopicGracefully() {
        // Given
        String topic = null;
        String message = "{\"userId\":\"user123\",\"message\":\"Test message\"}";

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            kafkaService.sendMessage(topic, message);
        });
        
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("Should send message with key and handle failure")
    void shouldSendMessageWithKeyAndHandleFailure() {
        // Given
        String topic = "test-topic";
        String key = "user123";
        String message = "{\"userId\":\"user123\",\"message\":\"Test message\"}";
        RuntimeException exception = new RuntimeException("Kafka send failed");
        
        CompletableFuture<SendResult<String, String>> failedFuture = CompletableFuture.failedFuture(exception);
        
        when(kafkaTemplate.send(eq(topic), eq(key), eq(message)))
            .thenReturn(failedFuture);

        // When & Then
        CompletableFuture<SendResult<String, String>> result = kafkaService.sendMessage(topic, key, message);
        
        assertThrows(ExecutionException.class, () -> {
            result.get();
        });
        
        verify(kafkaTemplate, times(1)).send(topic, key, message);
    }

    @Test
    @DisplayName("Should handle null key gracefully")
    void shouldHandleNullKeyGracefully() {
        // Given
        String topic = "test-topic";
        String key = null;
        String message = "{\"userId\":\"user123\",\"message\":\"Test message\"}";

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            kafkaService.sendMessage(topic, key, message);
        });
        
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
} 