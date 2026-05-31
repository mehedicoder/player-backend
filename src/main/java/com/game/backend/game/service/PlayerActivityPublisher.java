package com.game.backend.game.service;

/**
 * Publishes player activity events for state mutations.
 */
public interface PlayerActivityPublisher {
    /**
     * Publishes an event envelope to a fixed topic.
     */
    void publish(String topic, String key, PlayerActivityEventEnvelope event);
}

