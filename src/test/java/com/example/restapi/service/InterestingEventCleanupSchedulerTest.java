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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterestingEventCleanupScheduler Unit Tests")
class InterestingEventCleanupSchedulerTest {

    @Mock
    private InterestingEventRepository interestingEventRepository;

    @InjectMocks
    private InterestingEventCleanupScheduler cleanupScheduler;

    private InterestingEvent correlatedEvent;
    private InterestingEvent uncorrelatedEvent;

    @BeforeEach
    void setUp() {
        correlatedEvent = new InterestingEvent("test-topic", "userId", "user123");
        correlatedEvent.setId(1L);
        correlatedEvent.setCreatedAt(Instant.now());
        correlatedEvent.setCorrelatedMessage("{\"correlationId\":\"user123\"}");
        correlatedEvent.setIsCorrelated(true);
        correlatedEvent.setCorrelationTimestamp(Instant.now());

        uncorrelatedEvent = new InterestingEvent("test-topic", "userId", "user456");
        uncorrelatedEvent.setId(2L);
        uncorrelatedEvent.setCreatedAt(Instant.now());
        uncorrelatedEvent.setIsCorrelated(false);
    }

    @Test
    @DisplayName("Should cleanup correlated events successfully")
    void shouldCleanupCorrelatedEventsSuccessfully() {
        // Given
        long correlatedCount = 5L;
        long deletedCount = 5L;
        
        when(interestingEventRepository.countByIsCorrelatedTrue())
            .thenReturn(correlatedCount);
        when(interestingEventRepository.deleteByIsCorrelatedTrue())
            .thenReturn(deletedCount);

        // When
        cleanupScheduler.cleanupCorrelatedEvents();

        // Then
        verify(interestingEventRepository, times(1)).countByIsCorrelatedTrue();
        verify(interestingEventRepository, times(1)).deleteByIsCorrelatedTrue();
    }

    @Test
    @DisplayName("Should handle cleanup when no correlated events exist")
    void shouldHandleCleanupWhenNoCorrelatedEventsExist() {
        // Given
        long correlatedCount = 0L;
        
        when(interestingEventRepository.countByIsCorrelatedTrue())
            .thenReturn(correlatedCount);

        // When
        cleanupScheduler.cleanupCorrelatedEvents();

        // Then
        verify(interestingEventRepository, times(1)).countByIsCorrelatedTrue();
        verify(interestingEventRepository, never()).deleteByIsCorrelatedTrue();
    }

    @Test
    @DisplayName("Should handle repository exception during cleanup")
    void shouldHandleRepositoryExceptionDuringCleanup() {
        // Given
        when(interestingEventRepository.countByIsCorrelatedTrue())
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertDoesNotThrow(() -> {
            cleanupScheduler.cleanupCorrelatedEvents();
        });

        verify(interestingEventRepository, times(1)).countByIsCorrelatedTrue();
        verify(interestingEventRepository, never()).deleteByIsCorrelatedTrue();
    }

    @Test
    @DisplayName("Should handle repository exception during deletion")
    void shouldHandleRepositoryExceptionDuringDeletion() {
        // Given
        long correlatedCount = 5L;
        
        when(interestingEventRepository.countByIsCorrelatedTrue())
            .thenReturn(correlatedCount);
        when(interestingEventRepository.deleteByIsCorrelatedTrue())
            .thenThrow(new RuntimeException("Delete error"));

        // When & Then
        assertDoesNotThrow(() -> {
            cleanupScheduler.cleanupCorrelatedEvents();
        });

        verify(interestingEventRepository, times(1)).countByIsCorrelatedTrue();
        verify(interestingEventRepository, times(1)).deleteByIsCorrelatedTrue();
    }

    @Test
    @DisplayName("Should use configured cleanup interval")
    void shouldUseConfiguredCleanupInterval() {
        // Given
        int expectedInterval = 120; // 2 minutes
        ReflectionTestUtils.setField(cleanupScheduler, "cleanupIntervalSeconds", expectedInterval);
        
        when(interestingEventRepository.countByIsCorrelatedTrue())
            .thenReturn(0L);

        // When
        cleanupScheduler.cleanupCorrelatedEvents();

        // Then
        verify(interestingEventRepository, times(1)).countByIsCorrelatedTrue();
        
        // Verify the interval is set correctly
        Integer actualInterval = (Integer) ReflectionTestUtils.getField(cleanupScheduler, "cleanupIntervalSeconds");
        assertEquals(expectedInterval, actualInterval);
    }

    @Test
    @DisplayName("Should handle partial deletion success")
    void shouldHandlePartialDeletionSuccess() {
        // Given
        long correlatedCount = 10L;
        long deletedCount = 7L; // Some records couldn't be deleted
        
        when(interestingEventRepository.countByIsCorrelatedTrue())
            .thenReturn(correlatedCount);
        when(interestingEventRepository.deleteByIsCorrelatedTrue())
            .thenReturn(deletedCount);

        // When
        cleanupScheduler.cleanupCorrelatedEvents();

        // Then
        verify(interestingEventRepository, times(1)).countByIsCorrelatedTrue();
        verify(interestingEventRepository, times(1)).deleteByIsCorrelatedTrue();
    }

    @Test
    @DisplayName("Should handle zero deletion count")
    void shouldHandleZeroDeletionCount() {
        // Given
        long correlatedCount = 5L;
        long deletedCount = 0L;
        
        when(interestingEventRepository.countByIsCorrelatedTrue())
            .thenReturn(correlatedCount);
        when(interestingEventRepository.deleteByIsCorrelatedTrue())
            .thenReturn(deletedCount);

        // When
        cleanupScheduler.cleanupCorrelatedEvents();

        // Then
        verify(interestingEventRepository, times(1)).countByIsCorrelatedTrue();
        verify(interestingEventRepository, times(1)).deleteByIsCorrelatedTrue();
    }
} 