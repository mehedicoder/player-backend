package com.game.backend.game.service;

import com.game.backend.game.api.InventoryItemResponse;
import com.game.backend.game.api.InventoryMutationRequest;
import com.game.backend.game.api.InventoryOperation;
import com.game.backend.game.api.InventoryResponse;
import com.game.backend.game.domain.InventoryItem;
import com.game.backend.game.domain.InventoryMutationEvent;
import com.game.backend.game.repository.InventoryItemRepository;
import com.game.backend.game.repository.InventoryMutationEventRepository;
import com.game.backend.player.repository.PlayerProfileRepository;
import com.game.backend.player.service.PlayerNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Handles player inventory reads and mutations.
 */
@Service
public class InventoryService {

    private final InventoryItemRepository inventoryRepository;
    private final InventoryMutationEventRepository inventoryMutationEventRepository;
    private final PlayerProfileRepository playerRepository;
    private final TransactionalActivityPublisher activityPublisher;

    public InventoryService(
        InventoryItemRepository inventoryRepository,
        InventoryMutationEventRepository inventoryMutationEventRepository,
        PlayerProfileRepository playerRepository,
        TransactionalActivityPublisher activityPublisher
    ) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryMutationEventRepository = inventoryMutationEventRepository;
        this.playerRepository = playerRepository;
        this.activityPublisher = activityPublisher;
    }

    /**
     * Returns full inventory snapshot for player.
     */
    @Transactional(readOnly = true)
    public InventoryResponse getInventory(String playerId) {
        assertPlayerExists(playerId);
        List<InventoryItemResponse> items = inventoryRepository.findByPlayerIdOrderByItemCodeAsc(playerId)
            .stream()
            .map(i -> new InventoryItemResponse(i.getItemCode(), i.getQuantity()))
            .toList();
        return new InventoryResponse(playerId, items);
    }

    /**
     * Applies inventory add/remove operation.
     */
    @Transactional
    public InventoryResponse mutateInventory(String playerId, InventoryMutationRequest request) {
        assertPlayerExists(playerId);
        InventoryItem item = inventoryRepository.findByPlayerIdAndItemCode(playerId, request.itemCode())
            .orElseGet(() -> {
                InventoryItem created = new InventoryItem();
                created.setPlayerId(playerId);
                created.setItemCode(request.itemCode());
                created.setQuantity(0L);
                return created;
            });

        long current = item.getQuantity();
        long next = request.operation() == InventoryOperation.ADD
            ? current + request.quantity()
            : current - request.quantity();
        if (next < 0) {
            throw new IllegalArgumentException("Insufficient item quantity for remove operation");
        }
        item.setQuantity(next);
        InventoryItem persistedItem = inventoryRepository.save(item);
        long quantityDelta = request.operation() == InventoryOperation.ADD ? request.quantity() : -request.quantity();
        InventoryMutationEvent mutationEvent = new InventoryMutationEvent();
        mutationEvent.setPlayerId(playerId);
        mutationEvent.setInventoryItemId(persistedItem.getId());
        mutationEvent.setItemCode(request.itemCode());
        mutationEvent.setOperation(request.operation().name());
        mutationEvent.setQuantityDelta(quantityDelta);
        mutationEvent.setQuantityAfter(next);
        InventoryMutationEvent persistedMutationEvent = inventoryMutationEventRepository.save(mutationEvent);
        String mutationId = "inventory-mutation-event-" + persistedMutationEvent.getId();
        PlayerActivityEventEnvelope event = PlayerActivityEventEnvelope.v1(
            mutationId,
            PlayerActivityEventType.INVENTORY_MUTATED_V1,
            playerId,
            null,
            null,
            Map.of(
                "itemId", request.itemCode(),
                "operation", request.operation().name(),
                "quantityDelta", quantityDelta,
                "quantityAfter", next,
                "mutationId", mutationId
            )
        );
        activityPublisher.publishAfterCommit(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1, playerId, event);
        return getInventory(playerId);
    }

    private void assertPlayerExists(String playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }
    }
}
