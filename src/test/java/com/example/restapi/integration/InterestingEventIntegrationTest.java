package com.example.restapi.integration;

import com.example.restapi.entity.InterestingEvent;
import com.example.restapi.repository.InterestingEventRepository;
import com.example.restapi.service.InterestingEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
@DisplayName("InterestingEvent Integration Tests")
class InterestingEventIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private InterestingEventRepository interestingEventRepository;

    @Autowired
    private InterestingEventService interestingEventService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        // Clean up database before each test
        interestingEventRepository.deleteAll();
        interestingEventRepository.flush();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up database after each test
        interestingEventRepository.deleteAll();
        interestingEventRepository.flush();
    }

    @Test
    @DisplayName("Should create and retrieve interesting event through full stack")
    void shouldCreateAndRetrieveInterestingEventThroughFullStack() throws Exception {
        // Given
        String topicName = "test-topic";
        String keyName = "userId";
        String keyValue = "user123";

        // When - Create interesting event through service
        InterestingEvent createdEvent = interestingEventService.saveInterestingEvent(topicName, keyName, keyValue);

        // Then - Verify event was created
        assertNotNull(createdEvent);
        assertNotNull(createdEvent.getId());
        assertEquals(topicName, createdEvent.getTopicName());
        assertEquals(keyName, createdEvent.getKeyOfInterestName());
        assertEquals(keyValue, createdEvent.getKeyOfInterestValue());
        assertFalse(createdEvent.getIsCorrelated());
        assertNull(createdEvent.getCorrelatedMessage());
        assertNull(createdEvent.getCorrelationTimestamp());

        // When & Then - Retrieve through REST API
        mockMvc.perform(get("/api/v1/interesting-events/{id}", createdEvent.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.event.id").value(createdEvent.getId()))
            .andExpect(jsonPath("$.event.topicName").value(topicName))
            .andExpect(jsonPath("$.event.keyOfInterestName").value(keyName))
            .andExpect(jsonPath("$.event.keyOfInterestValue").value(keyValue))
            .andExpect(jsonPath("$.event.isCorrelated").value(false));
    }

    @Test
    @DisplayName("Should retrieve interesting events with pagination through REST API")
    void shouldRetrieveInterestingEventsWithPaginationThroughRestApi() throws Exception {
        // Given
        InterestingEvent event1 = interestingEventService.saveInterestingEvent("topic1", "key1", "value1");
        InterestingEvent event2 = interestingEventService.saveInterestingEvent("topic2", "key2", "value2");
        InterestingEvent event3 = interestingEventService.saveInterestingEvent("topic3", "key3", "value3");

        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events")
                .param("page", "0")
                .param("size", "2")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events.length()").value(2));

        mockMvc.perform(get("/api/v1/interesting-events")
                .param("page", "1")
                .param("size", "2")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events.length()").value(1));
    }

    @Test
    @DisplayName("Should find interesting events by topic name through REST API")
    void shouldFindInterestingEventsByTopicNameThroughRestApi() throws Exception {
        // Given
        String topicName = "test-topic";
        interestingEventService.saveInterestingEvent(topicName, "key1", "value1");
        interestingEventService.saveInterestingEvent(topicName, "key2", "value2");
        interestingEventService.saveInterestingEvent("other-topic", "key3", "value3");

        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events/by-topic")
                .param("topicName", topicName)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events.length()").value(2))
            .andExpect(jsonPath("$.events[0].topicName").value(topicName))
            .andExpect(jsonPath("$.events[1].topicName").value(topicName));
    }

    @Test
    @DisplayName("Should find interesting events by key of interest name through REST API")
    void shouldFindInterestingEventsByKeyOfInterestNameThroughRestApi() throws Exception {
        // Given
        String keyName = "userId";
        interestingEventService.saveInterestingEvent("topic1", keyName, "value1");
        interestingEventService.saveInterestingEvent("topic2", keyName, "value2");
        interestingEventService.saveInterestingEvent("topic3", "otherKey", "value3");

        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events/by-key")
                .param("keyOfInterestName", keyName)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events.length()").value(2))
            .andExpect(jsonPath("$.events[0].keyOfInterestName").value(keyName))
            .andExpect(jsonPath("$.events[1].keyOfInterestName").value(keyName));
    }

    @Test
    @DisplayName("Should get total count of interesting events through REST API")
    void shouldGetTotalCountOfInterestingEventsThroughRestApi() throws Exception {
        // Given
        interestingEventService.saveInterestingEvent("topic1", "key1", "value1");
        interestingEventService.saveInterestingEvent("topic2", "key2", "value2");
        interestingEventService.saveInterestingEvent("topic3", "key3", "value3");

        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events/stats")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEvents").value(3));
    }

    @Test
    @DisplayName("Should delete interesting event by ID through REST API")
    void shouldDeleteInterestingEventByIdThroughRestApi() throws Exception {
        // Given
        InterestingEvent event = interestingEventService.saveInterestingEvent("test-topic", "key1", "value1");
        Long eventId = event.getId();

        // Verify event exists
        assertTrue(interestingEventRepository.findById(eventId).isPresent());

        // When & Then
        mockMvc.perform(delete("/api/v1/interesting-events/{id}", eventId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Interesting event deleted successfully"));

        // Verify event was deleted
        assertFalse(interestingEventRepository.findById(eventId).isPresent());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent interesting event")
    void shouldReturn404WhenDeletingNonExistentInterestingEvent() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/interesting-events/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("No event found with ID: 999"));
    }

    @Test
    @DisplayName("Should return 404 when getting non-existent interesting event")
    void shouldReturn404WhenGettingNonExistentInterestingEvent() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle correlation logic through service")
    void shouldHandleCorrelationLogicThroughService() {
        // Given
        String keyName = "userId";
        String keyValue = "user123";
        String correlatedMessage = "{\"correlationId\":\"user123\"}";

        // Create interesting event
        InterestingEvent event = interestingEventService.saveInterestingEvent("test-topic", keyName, keyValue);
        
        // Verify initial state
        assertFalse(event.getIsCorrelated());
        assertNull(event.getCorrelatedMessage());
        assertNull(event.getCorrelationTimestamp());

        // When - Check correlation
        interestingEventService.checkAndUpdateCorrelation(keyName, keyValue, correlatedMessage);

        // Then - Verify correlation was updated
        InterestingEvent updatedEvent = interestingEventRepository.findById(event.getId()).orElse(null);
        assertNotNull(updatedEvent);
        assertTrue(updatedEvent.getIsCorrelated());
        assertEquals(correlatedMessage, updatedEvent.getCorrelatedMessage());
        assertNotNull(updatedEvent.getCorrelationTimestamp());
    }

    @Test
    @DisplayName("Should handle correlation when no match found")
    void shouldHandleCorrelationWhenNoMatchFound() {
        // Given
        String keyName = "userId";
        String keyValue = "nonexistent";
        String correlatedMessage = "{\"correlationId\":\"nonexistent\"}";

        // When - Check correlation for non-existent event
        interestingEventService.checkAndUpdateCorrelation(keyName, keyValue, correlatedMessage);

        // Then - Verify no events were created or updated
        List<InterestingEvent> events = interestingEventRepository.findAll();
        assertEquals(0, events.size());
    }

    @Test
    @DisplayName("Should handle invalid pagination parameters")
    void shouldHandleInvalidPaginationParameters() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events")
                .param("page", "-1")
                .param("size", "0")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle empty database gracefully")
    void shouldHandleEmptyDatabaseGracefully() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events.length()").value(0));
    }
} 