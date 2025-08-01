package com.example.restapi.controller;

import com.example.restapi.constants.ApiConstants;
import com.example.restapi.service.SqsService;
import com.example.restapi.service.SnsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_BASE_PATH)
@Tag(name = "API Controller", description = "Main API endpoints for health checks and basic operations")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final JdbcTemplate jdbcTemplate;
    private final SqsService sqsService;
    private final SnsService snsService;

    @Autowired
    public ApiController(JdbcTemplate jdbcTemplate, SqsService sqsService, SnsService snsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqsService = sqsService;
        this.snsService = snsService;
    }

    @GetMapping("/health")
    @Operation(
        summary = "Health Check",
        description = "Check the health status of the application including database connectivity"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application is healthy"),
        @ApiResponse(responseCode = "503", description = "Application is unhealthy")
    })
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test database connectivity with a simple query that works with HSQLDB
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES", Integer.class);
            
            response.put("status", "UP");
            response.put("database", "CONNECTED");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("Health check passed - database is connected");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Health check failed - database connection error", e);
            
            response.put("status", "DOWN");
            response.put("database", "DISCONNECTED");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(response);
        }
    }

    @GetMapping("/echo")
    @Operation(
        summary = "Echo Endpoint",
        description = "Returns the input string in a JSON response"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully returned the input string")
    })
    public ResponseEntity<Map<String, String>> echo(
        @Parameter(description = "Input string to echo back", example = "Hello World")
        @RequestParam(value = "inputString", defaultValue = "Hello World") String inputString) {
        
        Map<String, String> response = new HashMap<>();
        response.put("responseString", inputString);
        
        logger.info("Echo endpoint called with input: {}", inputString);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sqs/send")
    @Operation(
        summary = "Send SQS Message",
        description = "Send a message to AWS SQS queue"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message sent successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to send message")
    })
    public ResponseEntity<Map<String, Object>> sendSqsMessage(
        @Parameter(description = "Message to send to SQS", example = "Test message")
        @RequestBody Map<String, String> request) {
        
        try {
            String message = request.get("message");
            var response = sqsService.sendMessage(message);
            
            Map<String, Object> result = new HashMap<>();
            result.put("messageId", response.messageId());
            result.put("status", "SENT");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to send SQS message", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sqs/receive")
    @Operation(
        summary = "Receive SQS Messages",
        description = "Receive messages from AWS SQS queue"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Messages received successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to receive messages")
    })
    public ResponseEntity<Map<String, Object>> receiveSqsMessages(
        @Parameter(description = "Maximum number of messages to receive", example = "10")
        @RequestParam(value = "maxMessages", defaultValue = "10") int maxMessages) {
        
        try {
            var messages = sqsService.receiveMessages(maxMessages);
            
            Map<String, Object> result = new HashMap<>();
            result.put("messageCount", messages.size());
            result.put("messages", messages);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to receive SQS messages", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sns/publish")
    @Operation(
        summary = "Publish SNS Message",
        description = "Publish a message to AWS SNS topic"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message published successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to publish message")
    })
    public ResponseEntity<Map<String, Object>> publishSnsMessage(
        @Parameter(description = "Message to publish to SNS", example = "Test notification")
        @RequestBody Map<String, String> request) {
        
        try {
            String message = request.get("message");
            var response = snsService.publishMessage(message);
            
            Map<String, Object> result = new HashMap<>();
            result.put("messageId", response.messageId());
            result.put("status", "PUBLISHED");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to publish SNS message", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
} 