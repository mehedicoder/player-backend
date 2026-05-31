package com.game.backend.auth.service;

import java.time.OffsetDateTime;

/**
 * Session payload persisted for a logged-in player token.
 */
public record SessionData(
    String token,
    String playerId,
    OffsetDateTime issuedAt,
    OffsetDateTime expiresAt
) {
}
