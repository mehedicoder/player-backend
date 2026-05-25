package com.game.backend.game.api;

/**
 * Inventory item snapshot.
 */
public record InventoryItemResponse(
    String itemCode,
    long quantity
) {
}

