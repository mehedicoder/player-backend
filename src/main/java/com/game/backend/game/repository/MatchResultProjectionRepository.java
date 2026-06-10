package com.game.backend.game.repository;

import com.game.backend.game.domain.MatchResultProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Repository for consumed match-result projection rows.
 */
public interface MatchResultProjectionRepository extends JpaRepository<MatchResultProjection, Long> {

    /**
     * Finds the latest projection for one player and match identity.
     */
    Optional<MatchResultProjection> findByPlayerIdAndMatchId(String playerId, String matchId);

    /**
     * Atomically inserts or refreshes the projection only when the incoming event is not stale.
     */
    @Modifying
    @Query(value = """
        insert into match_result_projection (
            player_id,
            match_id,
            result,
            score_delta,
            event_id,
            occurred_at,
            created_at,
            updated_at
        )
        values (
            :playerId,
            :matchId,
            :result,
            :scoreDelta,
            :eventId,
            :occurredAt,
            utc_timestamp(),
            utc_timestamp()
        )
        on duplicate key update
            result = if(match_result_projection.occurred_at <= values(occurred_at), values(result), match_result_projection.result),
            score_delta = if(match_result_projection.occurred_at <= values(occurred_at), values(score_delta), match_result_projection.score_delta),
            event_id = if(match_result_projection.occurred_at <= values(occurred_at), values(event_id), match_result_projection.event_id),
            updated_at = if(match_result_projection.occurred_at <= values(occurred_at), utc_timestamp(), match_result_projection.updated_at),
            occurred_at = if(match_result_projection.occurred_at <= values(occurred_at), values(occurred_at), match_result_projection.occurred_at)
        """, nativeQuery = true)
    int upsertIfFresh(
        @Param("playerId") String playerId,
        @Param("matchId") String matchId,
        @Param("result") String result,
        @Param("scoreDelta") int scoreDelta,
        @Param("eventId") String eventId,
        @Param("occurredAt") OffsetDateTime occurredAt
    );
}
