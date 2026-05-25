package com.game.backend.game.repository;

import com.game.backend.game.domain.WalletLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for wallet ledger entries.
 */
public interface WalletLedgerRepository extends JpaRepository<WalletLedgerEntry, Long> {
    long countByPlayerId(String playerId);
}
