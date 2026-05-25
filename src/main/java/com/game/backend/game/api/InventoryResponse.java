package com.game.backend.game.api;

import java.util.List;

/**
 * Inventory response model.
 */
public record InventoryResponse(
    String playerId,
    List<InventoryItemResponse> items
) {
}

