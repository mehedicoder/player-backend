package com.game.backend.player.api;

import com.game.backend.player.domain.PlayerProfile;

import java.time.OffsetDateTime;

public record PlayerProfileResponse(
    String playerId,
    String displayName,
    String email,
    String countryCode,
    Integer level,
    Long experiencePoints,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static PlayerProfileResponse from(PlayerProfile profile) {
        return new PlayerProfileResponse(
            profile.getPlayerId(),
            profile.getDisplayName(),
            profile.getEmail(),
            profile.getCountryCode(),
            profile.getLevel(),
            profile.getExperiencePoints(),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }
}
