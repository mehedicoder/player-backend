package com.game.backend.game.repository;

import com.game.backend.game.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for player inventory records.
 */
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByPlayerIdOrderByItemCodeAsc(String playerId);
    Optional<InventoryItem> findByPlayerIdAndItemCode(String playerId, String itemCode);
}

