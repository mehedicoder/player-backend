package com.game.backend.game.repository;

import com.game.backend.game.domain.WalletIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for wallet idempotency records.
 */
public interface WalletIdempotencyRepository extends JpaRepository<WalletIdempotencyRecord, Long> {
    Optional<WalletIdempotencyRecord> findByPlayerIdAndIdempotencyKey(String playerId, String idempotencyKey);
    long countByPlayerId(String playerId);
}
