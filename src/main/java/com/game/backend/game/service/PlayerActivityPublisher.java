package com.game.backend.game.service;

/**
 * Publishes player activity events for state mutations.
 */
public interface PlayerActivityPublisher {
    /**
     * Publishes an event to a fixed topic.
     */
    void publish(String topic, String playerId, String action, String details);
}

