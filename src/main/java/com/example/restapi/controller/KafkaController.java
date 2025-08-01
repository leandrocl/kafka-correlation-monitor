package com.example.restapi.controller;

import com.example.restapi.constants.ApiConstants;
import com.example.restapi.dto.KafkaMessageRequest;
import com.example.restapi.service.KafkaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(ApiConstants.KAFKA_API_PATH)
@Tag(name = "Kafka Producer", description = "APIs for producing messages to Kafka topics")
public class KafkaController {

    private static final Logger logger = LoggerFactory.getLogger(KafkaController.class);

    private final KafkaService kafkaService;

    @Autowired
    public KafkaController(KafkaService kafkaService) {
        this.kafkaService = kafkaService;
    }

    @PostMapping("/produce")
    @Operation(
        summary = "Produce a message to a Kafka topic",
        description = "Sends a message to the specified Kafka topic"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Message sent successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request parameters"
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error or Kafka connection issue"
        )
    })
    public ResponseEntity<Map<String, Object>> produceMessage(
            @Parameter(description = "Kafka message request", required = true)
            @Valid @RequestBody KafkaMessageRequest request) {
        
        logger.info("Received Kafka message request: {}", request);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CompletableFuture<SendResult<String, String>> future = 
                kafkaService.sendMessage(request.getKafkaTopic(), request.getMessage());
            
            // Wait for the result (in a real application, you might want to handle this asynchronously)
            SendResult<String, String> result = future.get();
            
            response.put("status", "SUCCESS");
            response.put("message", "Message sent successfully to Kafka topic: " + request.getKafkaTopic());
            response.put("topic", request.getKafkaTopic());
            response.put("partition", result.getRecordMetadata().partition());
            response.put("offset", result.getRecordMetadata().offset());
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("Message sent successfully to topic '{}' at partition {} with offset {}", 
                request.getKafkaTopic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to send message to Kafka topic '{}': {}", request.getKafkaTopic(), e.getMessage(), e);
            
            response.put("status", "ERROR");
            response.put("message", "Failed to send message to Kafka topic: " + request.getKafkaTopic());
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/produce/simple")
    @Operation(
        summary = "Produce a message to a Kafka topic using query parameters",
        description = "Sends a message to the specified Kafka topic using query parameters"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Message sent successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request parameters"
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error or Kafka connection issue"
        )
    })
    public ResponseEntity<Map<String, Object>> produceMessageSimple(
            @Parameter(description = "Message to send", required = true)
            @RequestParam("message") String message,
            
            @Parameter(description = "Kafka topic to send the message to", required = true)
            @RequestParam("kafkaTopic") String kafkaTopic) {
        
        logger.info("Received simple Kafka message request - Topic: {}, Message: {}", kafkaTopic, message);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CompletableFuture<SendResult<String, String>> future = 
                kafkaService.sendMessage(kafkaTopic, message);
            
            // Wait for the result
            SendResult<String, String> result = future.get();
            
            response.put("status", "SUCCESS");
            response.put("message", "Message sent successfully to Kafka topic: " + kafkaTopic);
            response.put("topic", kafkaTopic);
            response.put("partition", result.getRecordMetadata().partition());
            response.put("offset", result.getRecordMetadata().offset());
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("Message sent successfully to topic '{}' at partition {} with offset {}", 
                kafkaTopic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to send message to Kafka topic '{}': {}", kafkaTopic, e.getMessage(), e);
            
            response.put("status", "ERROR");
            response.put("message", "Failed to send message to Kafka topic: " + kafkaTopic);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 