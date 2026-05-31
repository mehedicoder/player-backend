package com.game.backend.game.service;

/**
 * Kafka topic constants for player activity events.
 */
public final class PlayerActivityTopics {

    public static final String PLAYER_ACTIVITY_EVENTS_V1 = "player-activity-events.v1";
    public static final String PLAYER_ACTIVITY_EVENTS_V1_DLQ = "player-activity-events.v1.dlq";

    private PlayerActivityTopics() {
    }
}
