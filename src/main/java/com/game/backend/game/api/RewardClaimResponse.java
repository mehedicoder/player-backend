package com.game.backend.game.api;

/**
 * Reward claim response payload.
 */
public record RewardClaimResponse(
    String playerId,
    String rewardId,
    long rewardAmount,
    long balanceAfter
) {
}

