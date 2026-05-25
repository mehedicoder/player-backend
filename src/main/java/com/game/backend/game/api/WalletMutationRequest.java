package com.game.backend.game.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Wallet mutation command payload.
 */
public record WalletMutationRequest(
    @NotNull WalletMutationType mutationType,
    @Min(1) long amount,
    @NotBlank String idempotencyKey,
    @NotBlank String reason
) {
}

