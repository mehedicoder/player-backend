package com.game.backend.game.repository;

import com.game.backend.game.domain.LeaderboardScore;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository for leaderboard score state.
 */
public interface LeaderboardScoreRepository extends JpaRepository<LeaderboardScore, String> {

    /**
     * Locks leaderboard score row for consistent read-modify-write mutation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from LeaderboardScore s where s.playerId = :playerId")
    Optional<LeaderboardScore> findByPlayerIdForUpdate(@Param("playerId") String playerId);

    /**
     * Creates initial score row when absent.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO leaderboard_score (player_id, score, created_at, updated_at)
            VALUES (:playerId, 0, UTC_TIMESTAMP(), UTC_TIMESTAMP())
            ON DUPLICATE KEY UPDATE updated_at = updated_at
            """,
        nativeQuery = true
    )
    void ensureScoreRow(@Param("playerId") String playerId);
}
