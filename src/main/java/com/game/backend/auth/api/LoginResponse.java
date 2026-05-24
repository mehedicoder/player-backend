package com.game.backend.auth.api;

import java.time.OffsetDateTime;

/**
 * Login response containing opaque session token and expiration time.
 */
public record LoginResponse(
    String token,
    OffsetDateTime expiresAt
) {
}
