package com.game.backend.game.repository;

import com.game.backend.game.domain.InventoryMutationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisted inventory mutation event identities.
 */
public interface InventoryMutationEventRepository extends JpaRepository<InventoryMutationEvent, Long> {
}
