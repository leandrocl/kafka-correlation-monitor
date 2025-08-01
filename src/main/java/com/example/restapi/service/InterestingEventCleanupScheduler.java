package com.example.restapi.service;

import com.example.restapi.repository.InterestingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InterestingEventCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InterestingEventCleanupScheduler.class);

    private final InterestingEventRepository interestingEventRepository;

    @Value("${scheduler.cleanup.interval-seconds:60}")
    private int cleanupIntervalSeconds;

    @Autowired
    public InterestingEventCleanupScheduler(InterestingEventRepository interestingEventRepository) {
        this.interestingEventRepository = interestingEventRepository;
    }

    /**
     * Scheduled task to clean up correlated events from the interesting_events table
     * Runs every X seconds as configured in application.yml
     */
    @Scheduled(fixedDelayString = "${scheduler.cleanup.interval-seconds:60}000")
    public void cleanupCorrelatedEvents() {
        try {
            logger.info("Starting cleanup of correlated events from interesting_events table...");
            
            // Count records before deletion for logging
            long recordsBeforeDeletion = interestingEventRepository.countByIsCorrelatedTrue();
            
            if (recordsBeforeDeletion > 0) {
                // Delete all records where is_correlated = true
                long deletedCount = interestingEventRepository.deleteByIsCorrelatedTrue();
                
                logger.info("Cleanup completed successfully. Deleted {} correlated event records from interesting_events table.", deletedCount);
            } else {
                logger.info("No correlated events found to clean up.");
            }
            
        } catch (Exception e) {
            logger.error("Error during cleanup of correlated events: {}", e.getMessage(), e);
        }
    }
} 