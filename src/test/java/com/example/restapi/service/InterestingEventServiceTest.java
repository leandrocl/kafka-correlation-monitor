package com.example.restapi.service;

import com.example.restapi.entity.InterestingEvent;
import com.example.restapi.repository.InterestingEventRepository;
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
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterestingEventService Unit Tests")
class InterestingEventServiceTest {

    @Mock
    private InterestingEventRepository interestingEventRepository;

    @InjectMocks
    private InterestingEventService interestingEventService;

    private InterestingEvent sampleEvent;
    private InterestingEvent correlatedEvent;

    @BeforeEach
    void setUp() {
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
    @DisplayName("Should save interesting event successfully")
    void shouldSaveInterestingEvent() {
        // Given
        String topicName = "test-topic";
        String keyName = "userId";
        String keyValue = "user123";
        
        when(interestingEventRepository.save(any(InterestingEvent.class)))
            .thenReturn(sampleEvent);

        // When
        InterestingEvent result = interestingEventService.saveInterestingEvent(topicName, keyName, keyValue);

        // Then
        assertNotNull(result);
        assertEquals(topicName, result.getTopicName());
        assertEquals(keyName, result.getKeyOfInterestName());
        assertEquals(keyValue, result.getKeyOfInterestValue());
        assertFalse(result.getIsCorrelated());
        assertNull(result.getCorrelatedMessage());
        assertNull(result.getCorrelationTimestamp());
        
        verify(interestingEventRepository, times(1)).save(any(InterestingEvent.class));
    }

    @Test
    @DisplayName("Should find interesting events with pagination")
    void shouldFindInterestingEventsWithPagination() {
        // Given
        int offset = 0;
        int limit = 10;
        List<InterestingEvent> events = Arrays.asList(sampleEvent, correlatedEvent);
        
        when(interestingEventRepository.findAllWithOffset(offset, limit))
            .thenReturn(events);

        // When
        List<InterestingEvent> result = interestingEventService.findAllWithOffset(offset, limit);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(interestingEventRepository, times(1)).findAllWithOffset(offset, limit);
    }

    @Test
    @DisplayName("Should check and update correlation successfully")
    void shouldCheckAndUpdateCorrelation() {
        // Given
        String keyName = "userId";
        String keyValue = "user123";
        String correlatedMessage = "{\"correlationId\":\"user123\"}";
        List<InterestingEvent> events = Arrays.asList(sampleEvent);
        
        when(interestingEventRepository.findByKeyNameAndValueForCorrelation(keyName, keyValue))
            .thenReturn(events);
        when(interestingEventRepository.save(any(InterestingEvent.class)))
            .thenReturn(correlatedEvent);

        // When
        interestingEventService.checkAndUpdateCorrelation(keyName, keyValue, correlatedMessage);

        // Then
        verify(interestingEventRepository, times(1)).findByKeyNameAndValueForCorrelation(keyName, keyValue);
        verify(interestingEventRepository, times(1)).save(any(InterestingEvent.class));
    }

    @Test
    @DisplayName("Should handle no correlation found")
    void shouldHandleNoCorrelationFound() {
        // Given
        String keyName = "userId";
        String keyValue = "nonexistent";
        String correlatedMessage = "{\"correlationId\":\"nonexistent\"}";
        
        when(interestingEventRepository.findByKeyNameAndValueForCorrelation(keyName, keyValue))
            .thenReturn(Arrays.asList());

        // When
        interestingEventService.checkAndUpdateCorrelation(keyName, keyValue, correlatedMessage);

        // Then
        verify(interestingEventRepository, times(1)).findByKeyNameAndValueForCorrelation(keyName, keyValue);
        verify(interestingEventRepository, never()).save(any(InterestingEvent.class));
    }

    @Test
    @DisplayName("Should handle repository exception gracefully")
    void shouldHandleRepositoryException() {
        // Given
        String topicName = "test-topic";
        String keyName = "userId";
        String keyValue = "user123";
        
        when(interestingEventRepository.save(any(InterestingEvent.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            interestingEventService.saveInterestingEvent(topicName, keyName, keyValue);
        });
        
        verify(interestingEventRepository, times(1)).save(any(InterestingEvent.class));
    }

    @Test
    @DisplayName("Should count total interesting events")
    void shouldCountTotalInterestingEvents() {
        // Given
        long expectedCount = 10L;
        when(interestingEventRepository.countAllEvents()).thenReturn(expectedCount);

        // When
        long result = interestingEventService.countAllEvents();

        // Then
        assertEquals(expectedCount, result);
        verify(interestingEventRepository, times(1)).countAllEvents();
    }

    @Test
    @DisplayName("Should find interesting events by topic name with pagination")
    void shouldFindInterestingEventsByTopicName() {
        // Given
        String topicName = "test-topic";
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<InterestingEvent> events = Arrays.asList(sampleEvent);
        Page<InterestingEvent> eventPage = new PageImpl<>(events, pageable, events.size());
        
        when(interestingEventRepository.findByTopicNameOrderByCreatedAtDesc(topicName, pageable))
            .thenReturn(eventPage);

        // When
        Page<InterestingEvent> result = interestingEventService.findByTopicName(topicName, page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(sampleEvent, result.getContent().get(0));
        verify(interestingEventRepository, times(1)).findByTopicNameOrderByCreatedAtDesc(topicName, pageable);
    }

    @Test
    @DisplayName("Should find interesting events by key of interest name with pagination")
    void shouldFindInterestingEventsByKeyOfInterestName() {
        // Given
        String keyName = "userId";
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<InterestingEvent> events = Arrays.asList(sampleEvent);
        Page<InterestingEvent> eventPage = new PageImpl<>(events, pageable, events.size());
        
        when(interestingEventRepository.findByKeyOfInterestNameOrderByCreatedAtDesc(keyName, pageable))
            .thenReturn(eventPage);

        // When
        Page<InterestingEvent> result = interestingEventService.findByKeyOfInterestName(keyName, page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(sampleEvent, result.getContent().get(0));
        verify(interestingEventRepository, times(1)).findByKeyOfInterestNameOrderByCreatedAtDesc(keyName, pageable);
    }
} 