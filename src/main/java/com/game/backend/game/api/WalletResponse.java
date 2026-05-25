package com.game.backend.game.api;

import java.time.OffsetDateTime;

/**
 * Wallet read/mutation response payload.
 */
public record WalletResponse(
    String playerId,
    long balance,
    OffsetDateTime updatedAt
) {
}

