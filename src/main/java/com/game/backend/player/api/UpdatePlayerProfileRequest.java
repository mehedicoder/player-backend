package com.game.backend.player.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePlayerProfileRequest(
    @NotBlank
    @Size(max = 50)
    String displayName,
    @NotBlank
    @Email
    @Size(max = 190)
    String email,
    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}$", message = "countryCode must be ISO-3166 alpha-2 uppercase")
    String countryCode
) {
}
