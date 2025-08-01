package com.example.restapi.service;

import com.example.restapi.repository.InterestingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class InterestingEventMonitorScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InterestingEventMonitorScheduler.class);

    private final InterestingEventRepository interestingEventRepository;

    @Value("${scheduler.monitor.interval-seconds:30}")
    private int monitorIntervalSeconds;

    @Value("${scheduler.monitor.age-threshold-seconds:300}")
    private int ageThresholdSeconds;

    @Autowired
    public InterestingEventMonitorScheduler(InterestingEventRepository interestingEventRepository) {
        this.interestingEventRepository = interestingEventRepository;
    }

    /**
     * Scheduled task to monitor uncorrelated events older than the configured threshold
     * Runs every X seconds as configured in application.yml
     */
    @Scheduled(fixedDelayString = "${scheduler.monitor.interval-seconds:30}000")
    public void monitorUncorrelatedEvents() {
        try {
            logger.info("Starting monitoring of uncorrelated events older than {} seconds...", ageThresholdSeconds);
            
            // Calculate the threshold timestamp
            Instant threshold = Instant.now().minusSeconds(ageThresholdSeconds);
            
            // Find uncorrelated events older than threshold, grouped by topic
            List<Object[]> results = interestingEventRepository.findUncorrelatedEventsOlderThanGroupedByTopic(threshold);
            
            if (!results.isEmpty()) {
                logger.info("Found {} topics with uncorrelated events older than {} seconds:", results.size(), ageThresholdSeconds);
                
                for (Object[] result : results) {
                    String topicName = (String) result[0];
                    Long count = (Long) result[1];
                    logger.info("Topic: '{}' - Uncorrelated events count: {}", topicName, count);
                }
            } else {
                logger.info("No uncorrelated events found older than {} seconds.", ageThresholdSeconds);
            }
            
        } catch (Exception e) {
            logger.error("Error during monitoring of uncorrelated events: {}", e.getMessage(), e);
        }
    }
} 