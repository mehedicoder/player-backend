package com.game.backend.game.repository;

import com.game.backend.game.domain.PlayerWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Repository for wallet balance state.
 */
public interface PlayerWalletRepository extends JpaRepository<PlayerWallet, String> {

    /**
     * Locks wallet row for consistent read-modify-write updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from PlayerWallet w where w.playerId = :playerId")
    Optional<PlayerWallet> findByPlayerIdForUpdate(@Param("playerId") String playerId);

    /**
     * Creates initial wallet row when absent.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO player_wallet (player_id, balance, row_version, created_at, updated_at)
            VALUES (:playerId, 0, 0, UTC_TIMESTAMP(), UTC_TIMESTAMP())
            ON DUPLICATE KEY UPDATE updated_at = updated_at
            """,
        nativeQuery = true
    )
    void ensureWalletRow(@Param("playerId") String playerId);
}
