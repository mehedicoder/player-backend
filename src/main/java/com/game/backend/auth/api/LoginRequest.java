package com.game.backend.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request payload for phase-3 temporary credentials.
 */
public record LoginRequest(
    @NotBlank String playerId,
    @NotBlank @Email String email
) {
}
