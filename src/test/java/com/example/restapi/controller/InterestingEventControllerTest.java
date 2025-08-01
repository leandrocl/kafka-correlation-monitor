package com.example.restapi.controller;

import com.example.restapi.entity.InterestingEvent;
import com.example.restapi.service.InterestingEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterestingEventController Unit Tests")
class InterestingEventControllerTest {

    @Mock
    private InterestingEventService interestingEventService;

    @InjectMocks
    private InterestingEventController interestingEventController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private InterestingEvent sampleEvent;
    private InterestingEvent correlatedEvent;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(interestingEventController).build();
        objectMapper = new ObjectMapper();

        sampleEvent = new InterestingEvent("test-topic", "userId", "user123");
        sampleEvent.setId(1L);
        sampleEvent.setCreatedAt(Instant.now());

        correlatedEvent = new InterestingEvent("test-topic", "userId", "user123");
        correlatedEvent.setId(2L);
        correlatedEvent.setCreatedAt(Instant.now());
        correlatedEvent.setCorrelatedMessage("{\"correlationId\":\"user123\"}");
        correlatedEvent.setIsCorrelated(true);
        correlatedEvent.setCorrelationTimestamp(Instant.now());
    }

    @Test
    @DisplayName("Should get interesting events with pagination")
    void shouldGetInterestingEventsWithPagination() throws Exception {
        // Given
        int offset = 0;
        int limit = 10;
        List<InterestingEvent> events = Arrays.asList(sampleEvent, correlatedEvent);
        
        when(interestingEventService.findAllWithOffset(offset, limit))
            .thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events")
                .param("offset", String.valueOf(offset))
                .param("limit", String.valueOf(limit))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].topicName").value("test-topic"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].isCorrelated").value(true));

        verify(interestingEventService, times(1)).findAllWithOffset(offset, limit);
    }

    @Test
    @DisplayName("Should get interesting event by ID")
    void shouldGetInterestingEventById() throws Exception {
        // Given
        Long eventId = 1L;
        
        when(interestingEventService.findById(eventId))
            .thenReturn(Optional.of(sampleEvent));

        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events/{id}", eventId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.topicName").value("test-topic"))
            .andExpect(jsonPath("$.keyOfInterestName").value("userId"))
            .andExpect(jsonPath("$.keyOfInterestValue").value("user123"));

        verify(interestingEventService, times(1)).findById(eventId);
    }

    @Test
    @DisplayName("Should return 404 when interesting event not found")
    void shouldReturn404WhenInterestingEventNotFound() throws Exception {
        // Given
        Long eventId = 999L;
        
        when(interestingEventService.findById(eventId))
            .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events/{id}", eventId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(interestingEventService, times(1)).findById(eventId);
    }

    @Test
    @DisplayName("Should get interesting events by topic name")
    void shouldGetInterestingEventsByTopicName() throws Exception {
        // Given
        String topicName = "test-topic";
        int page = 0;
        int size = 10;
        List<InterestingEvent> events = Arrays.asList(sampleEvent);
        Page<InterestingEvent> eventPage = new PageImpl<>(events, PageRequest.of(page, size), events.size());
        
        when(interestingEventService.findByTopicName(topicName, page, size))
            .thenReturn(eventPage);

        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events/topic/{topicName}", topicName)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].topicName").value("test-topic"));

        verify(interestingEventService, times(1)).findByTopicName(topicName, page, size);
    }

    @Test
    @DisplayName("Should get interesting events by key of interest name")
    void shouldGetInterestingEventsByKeyOfInterestName() throws Exception {
        // Given
        String keyName = "userId";
        int page = 0;
        int size = 10;
        List<InterestingEvent> events = Arrays.asList(sampleEvent);
        Page<InterestingEvent> eventPage = new PageImpl<>(events, PageRequest.of(page, size), events.size());
        
        when(interestingEventService.findByKeyOfInterestName(keyName, page, size))
            .thenReturn(eventPage);

        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events/key/{keyName}", keyName)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].keyOfInterestName").value("userId"));

        verify(interestingEventService, times(1)).findByKeyOfInterestName(keyName, page, size);
    }

    @Test
    @DisplayName("Should get total count of interesting events")
    void shouldGetTotalCountOfInterestingEvents() throws Exception {
        // Given
        long expectedCount = 10L;
        
        when(interestingEventService.countAllEvents())
            .thenReturn(expectedCount);

        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events/count")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(expectedCount));

        verify(interestingEventService, times(1)).countAllEvents();
    }

    @Test
    @DisplayName("Should delete interesting event by ID")
    void shouldDeleteInterestingEventById() throws Exception {
        // Given
        Long eventId = 1L;
        
        when(interestingEventService.deleteById(eventId))
            .thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/v1/interesting-events/{id}", eventId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Interesting event deleted successfully"));

        verify(interestingEventService, times(1)).deleteById(eventId);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent interesting event")
    void shouldReturn404WhenDeletingNonExistentInterestingEvent() throws Exception {
        // Given
        Long eventId = 999L;
        
        when(interestingEventService.deleteById(eventId))
            .thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/v1/interesting-events/{id}", eventId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Interesting event not found"));

        verify(interestingEventService, times(1)).deleteById(eventId);
    }

    @Test
    @DisplayName("Should handle invalid pagination parameters")
    void shouldHandleInvalidPaginationParameters() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events")
                .param("offset", "-1")
                .param("limit", "0")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        verify(interestingEventService, never()).findAllWithOffset(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should handle service exception gracefully")
    void shouldHandleServiceExceptionGracefully() throws Exception {
        // Given
        Long eventId = 1L;
        
        when(interestingEventService.findById(eventId))
            .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/api/v1/interesting-events/{id}", eventId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());

        verify(interestingEventService, times(1)).findById(eventId);
    }
} 