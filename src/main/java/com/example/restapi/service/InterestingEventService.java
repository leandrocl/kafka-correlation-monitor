package com.example.restapi.service;

import com.example.restapi.entity.InterestingEvent;
import com.example.restapi.repository.InterestingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InterestingEventService {

    private static final Logger logger = LoggerFactory.getLogger(InterestingEventService.class);

    private final InterestingEventRepository interestingEventRepository;

    @Autowired
    public InterestingEventService(InterestingEventRepository interestingEventRepository) {
        this.interestingEventRepository = interestingEventRepository;
    }

    /**
     * Save an interesting event to the database
     */
    public InterestingEvent saveInterestingEvent(String topicName, String keyOfInterestName, String keyOfInterestValue) {
        try {
            InterestingEvent event = new InterestingEvent(topicName, keyOfInterestName, keyOfInterestValue);
            InterestingEvent savedEvent = interestingEventRepository.save(event);
            
            logger.info("Saved interesting event - Topic: '{}', Key: '{}', Value: '{}', ID: {}", 
                topicName, keyOfInterestName, keyOfInterestValue, savedEvent.getId());
            
            return savedEvent;
        } catch (Exception e) {
            logger.error("Error saving interesting event - Topic: '{}', Key: '{}', Value: '{}'", 
                topicName, keyOfInterestName, keyOfInterestValue, e);
            throw e;
        }
    }

    /**
     * Find interesting event by ID
     */
    public Optional<InterestingEvent> findById(Long id) {
        return interestingEventRepository.findById(id);
    }

    /**
     * Get all interesting events with pagination
     */
    public Page<InterestingEvent> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return interestingEventRepository.findAll(pageable);
    }

    /**
     * Get interesting events by topic name with pagination
     */
    public Page<InterestingEvent> findByTopicName(String topicName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return interestingEventRepository.findByTopicNameOrderByCreatedAtDesc(topicName, pageable);
    }

    /**
     * Get interesting events by key of interest name with pagination
     */
    public Page<InterestingEvent> findByKeyOfInterestName(String keyOfInterestName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return interestingEventRepository.findByKeyOfInterestNameOrderByCreatedAtDesc(keyOfInterestName, pageable);
    }

    /**
     * Get interesting events by topic name and key of interest name with pagination
     */
    public Page<InterestingEvent> findByTopicNameAndKeyOfInterestName(String topicName, String keyOfInterestName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return interestingEventRepository.findByTopicNameAndKeyOfInterestNameOrderByCreatedAtDesc(topicName, keyOfInterestName, pageable);
    }

    /**
     * Get interesting events created after a specific timestamp with pagination
     */
    public Page<InterestingEvent> findByCreatedAtAfter(Instant createdAt, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return interestingEventRepository.findByCreatedAtAfterOrderByCreatedAtDesc(createdAt, pageable);
    }

    /**
     * Get interesting events created between two timestamps with pagination
     */
    public Page<InterestingEvent> findByCreatedAtBetween(Instant startTime, Instant endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return interestingEventRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startTime, endTime, pageable);
    }

    /**
     * Get interesting events with offset-based pagination
     */
    public List<InterestingEvent> findAllWithOffset(int offset, int limit) {
        return interestingEventRepository.findAllWithOffset(offset, limit);
    }

    /**
     * Count total interesting events
     */
    public long countAllEvents() {
        return interestingEventRepository.countAllEvents();
    }

    /**
     * Count interesting events by topic name
     */
    public long countByTopicName(String topicName) {
        return interestingEventRepository.countByTopicName(topicName);
    }

    /**
     * Count interesting events by key of interest name
     */
    public long countByKeyOfInterestName(String keyOfInterestName) {
        return interestingEventRepository.countByKeyOfInterestName(keyOfInterestName);
    }

    /**
     * Delete interesting event by ID
     */
    public boolean deleteById(Long id) {
        if (interestingEventRepository.existsById(id)) {
            interestingEventRepository.deleteById(id);
            logger.info("Deleted interesting event with ID: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Check for correlation and update if match is found
     */
    public void checkAndUpdateCorrelation(String keyName, String keyValue, String correlatedMessage) {
        try {
            List<InterestingEvent> matchingEvents = interestingEventRepository.findByKeyNameAndValueForCorrelation(keyName, keyValue);
            
            if (!matchingEvents.isEmpty()) {
                InterestingEvent matchedEvent = matchingEvents.get(0); // Get the most recent match
                
                // Update the matched event with correlation data
                matchedEvent.setCorrelatedMessage(correlatedMessage);
                matchedEvent.setIsCorrelated(true);
                matchedEvent.setCorrelationTimestamp(Instant.now());
                
                InterestingEvent updatedEvent = interestingEventRepository.save(matchedEvent);
                
                logger.info("CORRELATION MATCH FOUND! Event ID: {}, Original Topic: '{}', Key: '{}', Value: '{}', " +
                    "Original Message: '{}', Correlated Message: '{}', Correlation Time: {}", 
                    updatedEvent.getId(), updatedEvent.getTopicName(), updatedEvent.getKeyOfInterestName(), 
                    updatedEvent.getKeyOfInterestValue(), updatedEvent.getCorrelatedMessage(), correlatedMessage, 
                    updatedEvent.getCorrelationTimestamp());
            } else {
                logger.info("No correlation match found for key: '{}' with value: '{}'", keyName, keyValue);
            }
        } catch (Exception e) {
            logger.error("Error checking correlation for key: '{}' with value: '{}'", keyName, keyValue, e);
        }
    }
} 