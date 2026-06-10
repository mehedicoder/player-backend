package com.game.backend.game.repository;

import com.game.backend.game.domain.MatchResultProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for match-result processed-event dedupe markers.
 */
public interface MatchResultProcessedEventRepository extends JpaRepository<MatchResultProcessedEvent, Long> {

    /**
     * Returns true when this consumer group already processed the event identifier.
     */
    boolean existsByConsumerGroupAndEventId(String consumerGroup, String eventId);

    /**
     * Counts persisted dedupe markers for one consumer group and event identifier.
     */
    long countByConsumerGroupAndEventId(String consumerGroup, String eventId);

    /**
     * Inserts the dedupe marker if absent and returns 1 when this consumer won processing ownership.
     */
    @Modifying
    @Query(value = """
        insert ignore into match_result_processed_event (consumer_group, event_id, event_type, player_id, created_at)
        values (:consumerGroup, :eventId, :eventType, :playerId, utc_timestamp())
        """, nativeQuery = true)
    int insertIgnoreProcessedEvent(
        @Param("consumerGroup") String consumerGroup,
        @Param("eventId") String eventId,
        @Param("eventType") String eventType,
        @Param("playerId") String playerId
    );
}
