package com.game.backend.game.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Inventory mutation command payload.
 */
public record InventoryMutationRequest(
    @NotBlank String itemCode,
    @NotNull InventoryOperation operation,
    @Min(1) long quantity
) {
}

