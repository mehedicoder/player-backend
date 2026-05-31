package com.game.backend.game.repository;

import com.game.backend.game.domain.LeaderboardProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for processed event dedupe markers.
 */
public interface LeaderboardProcessedEventRepository extends JpaRepository<LeaderboardProcessedEvent, Long> {

    /**
     * Returns true when this consumer group already processed the event identifier.
     */
    boolean existsByConsumerGroupAndEventId(String consumerGroup, String eventId);

    /**
     * Counts persisted dedupe markers for one consumer group and event identifier.
     */
    long countByConsumerGroupAndEventId(String consumerGroup, String eventId);
}
