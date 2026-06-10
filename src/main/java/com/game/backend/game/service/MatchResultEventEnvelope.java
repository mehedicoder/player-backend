package com.game.backend.game.service;

import java.util.Map;

/**
 * Versioned envelope for consumed match-result events.
 *
 * @param eventId unique event identifier for consumer dedupe
 * @param eventType semantic event type
 * @param playerId single-player aggregate key
 * @param occurredAt event timestamp
 * @param payload non-sensitive match-result payload
 */
public record MatchResultEventEnvelope(
    String eventId,
    String eventType,
    String playerId,
    String occurredAt,
    Map<String, Object> payload
) {
}
