package com.game.backend.game.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Reward claim command payload.
 */
public record RewardClaimRequest(
    @NotBlank String idempotencyKey,
    @Min(1) long rewardAmount
) {
}

