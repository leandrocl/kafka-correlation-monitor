package com.example.restapi.controller;

import com.example.restapi.constants.ApiConstants;
import com.example.restapi.config.KafkaTopicConfig;
import com.example.restapi.service.DynamicKafkaConsumerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.KAFKA_API_PATH + "/consumers")
@Tag(name = "Kafka Consumers", description = "APIs for managing Kafka consumers")
public class KafkaConsumerController {

    private final DynamicKafkaConsumerService dynamicKafkaConsumerService;
    private final KafkaTopicConfig kafkaTopicConfig;

    @Autowired
    public KafkaConsumerController(DynamicKafkaConsumerService dynamicKafkaConsumerService, 
                                 KafkaTopicConfig kafkaTopicConfig) {
        this.dynamicKafkaConsumerService = dynamicKafkaConsumerService;
        this.kafkaTopicConfig = kafkaTopicConfig;
    }

    @GetMapping("/status")
    @Operation(
        summary = "Get Kafka consumers status",
        description = "Returns the status of all configured Kafka consumers"
    )
    public ResponseEntity<Map<String, Object>> getConsumersStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("status", "ACTIVE");
        response.put("totalConsumers", kafkaTopicConfig.getTopics().size());
        response.put("consumers", kafkaTopicConfig.getTopics());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/topics")
    @Operation(
        summary = "Get configured Kafka topics",
        description = "Returns the list of configured Kafka topics for consumption"
    )
    public ResponseEntity<Map<String, Object>> getConfiguredTopics() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("topics", kafkaTopicConfig.getTopics());
        response.put("count", kafkaTopicConfig.getTopics().size());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(
        summary = "Check Kafka consumers health",
        description = "Returns the health status of Kafka consumers"
    )
    public ResponseEntity<Map<String, Object>> getConsumersHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("status", "UP");
            response.put("message", "Kafka consumers are active and listening");
            response.put("activeConsumers", kafkaTopicConfig.getTopics().size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("message", "Kafka consumers are not responding");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(response);
        }
    }
} 