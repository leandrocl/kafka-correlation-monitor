package com.example.restapi.controller;

import com.example.restapi.constants.ApiConstants;
import com.example.restapi.entity.InterestingEvent;
import com.example.restapi.service.InterestingEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_BASE_PATH + "/interesting-events")
@Tag(name = "Interesting Events", description = "APIs for managing interesting events from Kafka messages")
public class InterestingEventController {

    private final InterestingEventService interestingEventService;

    @Autowired
    public InterestingEventController(InterestingEventService interestingEventService) {
        this.interestingEventService = interestingEventService;
    }

    @GetMapping
    @Operation(
        summary = "Get all interesting events with pagination",
        description = "Retrieves all interesting events with page-based pagination"
    )
    public ResponseEntity<Map<String, Object>> getAllInterestingEvents(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        try {
            Page<InterestingEvent> eventsPage = interestingEventService.findAll(page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("events", eventsPage.getContent());
            response.put("currentPage", eventsPage.getNumber());
            response.put("totalPages", eventsPage.getTotalPages());
            response.put("totalElements", eventsPage.getTotalElements());
            response.put("hasNext", eventsPage.hasNext());
            response.put("hasPrevious", eventsPage.hasPrevious());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve interesting events");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/offset")
    @Operation(
        summary = "Get interesting events with offset-based pagination",
        description = "Retrieves interesting events using offset-based pagination"
    )
    public ResponseEntity<Map<String, Object>> getInterestingEventsWithOffset(
            @Parameter(description = "Offset (number of records to skip)", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            
            @Parameter(description = "Limit (number of records to return)", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        try {
            List<InterestingEvent> events = interestingEventService.findAllWithOffset(offset, limit);
            long totalCount = interestingEventService.countAllEvents();
            
            Map<String, Object> response = new HashMap<>();
            response.put("events", events);
            response.put("offset", offset);
            response.put("limit", limit);
            response.put("totalCount", totalCount);
            response.put("hasMore", (offset + limit) < totalCount);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve interesting events with offset");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/by-topic")
    @Operation(
        summary = "Get interesting events by topic name",
        description = "Retrieves interesting events filtered by topic name with pagination"
    )
    public ResponseEntity<Map<String, Object>> getInterestingEventsByTopic(
            @Parameter(description = "Topic name", example = "test-topic")
            @RequestParam String topicName,
            
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        try {
            Page<InterestingEvent> eventsPage = interestingEventService.findByTopicName(topicName, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("events", eventsPage.getContent());
            response.put("topicName", topicName);
            response.put("currentPage", eventsPage.getNumber());
            response.put("totalPages", eventsPage.getTotalPages());
            response.put("totalElements", eventsPage.getTotalElements());
            response.put("hasNext", eventsPage.hasNext());
            response.put("hasPrevious", eventsPage.hasPrevious());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve interesting events by topic");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/by-key")
    @Operation(
        summary = "Get interesting events by key of interest name",
        description = "Retrieves interesting events filtered by key of interest name with pagination"
    )
    public ResponseEntity<Map<String, Object>> getInterestingEventsByKey(
            @Parameter(description = "Key of interest name", example = "userId")
            @RequestParam String keyOfInterestName,
            
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        try {
            Page<InterestingEvent> eventsPage = interestingEventService.findByKeyOfInterestName(keyOfInterestName, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("events", eventsPage.getContent());
            response.put("keyOfInterestName", keyOfInterestName);
            response.put("currentPage", eventsPage.getNumber());
            response.put("totalPages", eventsPage.getTotalPages());
            response.put("totalElements", eventsPage.getTotalElements());
            response.put("hasNext", eventsPage.hasNext());
            response.put("hasPrevious", eventsPage.hasPrevious());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve interesting events by key");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/by-topic-and-key")
    @Operation(
        summary = "Get interesting events by topic name and key of interest name",
        description = "Retrieves interesting events filtered by both topic name and key of interest name with pagination"
    )
    public ResponseEntity<Map<String, Object>> getInterestingEventsByTopicAndKey(
            @Parameter(description = "Topic name", example = "test-topic")
            @RequestParam String topicName,
            
            @Parameter(description = "Key of interest name", example = "userId")
            @RequestParam String keyOfInterestName,
            
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        try {
            Page<InterestingEvent> eventsPage = interestingEventService.findByTopicNameAndKeyOfInterestName(topicName, keyOfInterestName, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("events", eventsPage.getContent());
            response.put("topicName", topicName);
            response.put("keyOfInterestName", keyOfInterestName);
            response.put("currentPage", eventsPage.getNumber());
            response.put("totalPages", eventsPage.getTotalPages());
            response.put("totalElements", eventsPage.getTotalElements());
            response.put("hasNext", eventsPage.hasNext());
            response.put("hasPrevious", eventsPage.hasPrevious());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve interesting events by topic and key");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/by-date-range")
    @Operation(
        summary = "Get interesting events by date range",
        description = "Retrieves interesting events created between two timestamps with pagination"
    )
    public ResponseEntity<Map<String, Object>> getInterestingEventsByDateRange(
            @Parameter(description = "Start time (ISO-8601 format)", example = "2025-07-31T00:00:00Z")
            @RequestParam String startTime,
            
            @Parameter(description = "End time (ISO-8601 format)", example = "2025-07-31T23:59:59Z")
            @RequestParam String endTime,
            
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        try {
            Instant start = Instant.parse(startTime);
            Instant end = Instant.parse(endTime);
            
            Page<InterestingEvent> eventsPage = interestingEventService.findByCreatedAtBetween(start, end, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("events", eventsPage.getContent());
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("currentPage", eventsPage.getNumber());
            response.put("totalPages", eventsPage.getTotalPages());
            response.put("totalElements", eventsPage.getTotalElements());
            response.put("hasNext", eventsPage.hasNext());
            response.put("hasPrevious", eventsPage.hasPrevious());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (DateTimeParseException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid date format");
            errorResponse.put("message", "Please use ISO-8601 format (e.g., 2025-07-31T00:00:00Z)");
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve interesting events by date range");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get interesting event by ID",
        description = "Retrieves a specific interesting event by its ID"
    )
    public ResponseEntity<Map<String, Object>> getInterestingEventById(
            @Parameter(description = "Event ID", example = "1")
            @PathVariable Long id) {

        try {
            return interestingEventService.findById(id)
                    .map(event -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("event", event);
                        response.put("timestamp", System.currentTimeMillis());
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Interesting event not found");
                        errorResponse.put("message", "No event found with ID: " + id);
                        errorResponse.put("timestamp", System.currentTimeMillis());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                    });
                    
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve interesting event");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete interesting event by ID",
        description = "Deletes a specific interesting event by its ID"
    )
    public ResponseEntity<Map<String, Object>> deleteInterestingEventById(
            @Parameter(description = "Event ID", example = "1")
            @PathVariable Long id) {

        try {
            boolean deleted = interestingEventService.deleteById(id);
            
            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Interesting event deleted successfully");
                response.put("id", id);
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Interesting event not found");
                errorResponse.put("message", "No event found with ID: " + id);
                errorResponse.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to delete interesting event");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/stats")
    @Operation(
        summary = "Get interesting events statistics",
        description = "Retrieves statistics about interesting events"
    )
    public ResponseEntity<Map<String, Object>> getInterestingEventsStats() {

        try {
            long totalEvents = interestingEventService.countAllEvents();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalEvents", totalEvents);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve interesting events statistics");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
} 