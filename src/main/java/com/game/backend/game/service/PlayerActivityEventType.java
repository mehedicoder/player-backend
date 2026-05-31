package com.game.backend.game.service;

/**
 * Event type constants for player activity envelope schema v1.
 */
public final class PlayerActivityEventType {

    public static final String WALLET_CREDITED_V1 = "wallet.credited.v1";
    public static final String WALLET_DEBITED_V1 = "wallet.debited.v1";
    public static final String INVENTORY_MUTATED_V1 = "inventory.mutated.v1";
    public static final String REWARD_CLAIMED_V1 = "reward.claimed.v1";

    private PlayerActivityEventType() {
    }
}
