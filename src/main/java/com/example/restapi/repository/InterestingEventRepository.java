package com.example.restapi.repository;

import com.example.restapi.entity.InterestingEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InterestingEventRepository extends JpaRepository<InterestingEvent, Long> {

    /**
     * Find interesting events by topic name with pagination
     */
    Page<InterestingEvent> findByTopicNameOrderByCreatedAtDesc(String topicName, Pageable pageable);

    /**
     * Find interesting events by key of interest name with pagination
     */
    Page<InterestingEvent> findByKeyOfInterestNameOrderByCreatedAtDesc(String keyOfInterestName, Pageable pageable);

    /**
     * Find interesting events by topic name and key of interest name with pagination
     */
    Page<InterestingEvent> findByTopicNameAndKeyOfInterestNameOrderByCreatedAtDesc(
            String topicName, String keyOfInterestName, Pageable pageable);

    /**
     * Find interesting events created after a specific timestamp with pagination
     */
    Page<InterestingEvent> findByCreatedAtAfterOrderByCreatedAtDesc(Instant createdAt, Pageable pageable);

    /**
     * Find interesting events created between two timestamps with pagination
     */
    Page<InterestingEvent> findByCreatedAtBetweenOrderByCreatedAtDesc(
            Instant startTime, Instant endTime, Pageable pageable);

    /**
     * Find interesting events with offset-based pagination
     */
    @Query(value = "SELECT * FROM interesting_events ORDER BY created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<InterestingEvent> findAllWithOffset(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * Count total interesting events
     */
    @Query("SELECT COUNT(ie) FROM InterestingEvent ie")
    long countAllEvents();

    /**
     * Count interesting events by topic name
     */
    long countByTopicName(String topicName);

    /**
     * Count interesting events by key of interest name
     */
    long countByKeyOfInterestName(String keyOfInterestName);

    /**
     * Find interesting events by key name and value for correlation matching
     */
    @Query("SELECT ie FROM InterestingEvent ie WHERE ie.keyOfInterestName = :keyName AND ie.keyOfInterestValue = :keyValue AND ie.isCorrelated = false ORDER BY ie.createdAt DESC")
    List<InterestingEvent> findByKeyNameAndValueForCorrelation(@Param("keyName") String keyName, @Param("keyValue") String keyValue);

    /**
     * Count interesting events where isCorrelated is true
     */
    long countByIsCorrelatedTrue();

    /**
     * Delete interesting events where isCorrelated is true
     */
    long deleteByIsCorrelatedTrue();

    /**
     * Find uncorrelated events older than specified timestamp, grouped by topic name
     */
    @Query("SELECT ie.topicName, COUNT(ie) FROM InterestingEvent ie " +
           "WHERE ie.isCorrelated = false AND ie.createdAt < :threshold " +
           "GROUP BY ie.topicName " +
           "HAVING COUNT(ie) > 0")
    List<Object[]> findUncorrelatedEventsOlderThanGroupedByTopic(@Param("threshold") Instant threshold);
} 