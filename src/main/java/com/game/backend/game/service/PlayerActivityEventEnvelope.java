package com.game.backend.game.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;

/**
 * Versioned envelope for player activity events.
 *
 * @param eventId unique event identifier for consumer dedupe
 * @param eventType semantic event type
 * @param schemaVersion schema version marker
 * @param playerId aggregate key used as Kafka message key
 * @param occurredAt event timestamp in UTC
 * @param source producing service identifier
 * @param correlationId optional request correlation id
 * @param idempotencyKey optional idempotency key from business operation
 * @param payload non-sensitive event-specific payload
 */
public record PlayerActivityEventEnvelope(
    String eventId,
    String eventType,
    String schemaVersion,
    String playerId,
    String occurredAt,
    String source,
    String correlationId,
    String idempotencyKey,
    Map<String, Object> payload
) {

    public static final String SCHEMA_VERSION_V1 = "v1";
    public static final String SOURCE = "player-backend";

    /**
     * Builds a v1 envelope with UTC timestamp.
     */
    public static PlayerActivityEventEnvelope v1(
        String eventId,
        String eventType,
        String playerId,
        String correlationId,
        String idempotencyKey,
        Map<String, Object> payload
    ) {
        String safeEventId = requireNonBlank("eventId", eventId);
        String safeEventType = requireNonBlank("eventType", eventType);
        String safePlayerId = requireNonBlank("playerId", playerId);
        Map<String, Object> safePayload = Map.copyOf(Objects.requireNonNull(payload, "payload must not be null"));
        return new PlayerActivityEventEnvelope(
            safeEventId,
            safeEventType,
            SCHEMA_VERSION_V1,
            safePlayerId,
            OffsetDateTime.now(ZoneOffset.UTC).toString(),
            SOURCE,
            correlationId,
            idempotencyKey,
            safePayload
        );
    }

    private static String requireNonBlank(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
