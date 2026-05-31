package com.game.backend.game.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Kafka adapter that consumes player activity events for leaderboard updates.
 */
@Component
public class PlayerActivityLeaderboardConsumer {

    private static final Logger log = LoggerFactory.getLogger(PlayerActivityLeaderboardConsumer.class);

    private final ObjectMapper objectMapper;
    private final LeaderboardScoreUpdateService leaderboardScoreUpdateService;
    private final LeaderboardConsumerProperties properties;
    private final MeterRegistry meterRegistry;

    public PlayerActivityLeaderboardConsumer(
        ObjectMapper objectMapper,
        LeaderboardScoreUpdateService leaderboardScoreUpdateService,
        LeaderboardConsumerProperties properties,
        MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.leaderboardScoreUpdateService = leaderboardScoreUpdateService;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Consumes one activity event with manual ack and explicit invalid-event handling.
     */
    @KafkaListener(
        topics = "${leaderboard.consumer.topic:player-activity-events.v1}",
        groupId = "${leaderboard.consumer.group-id:leaderboard-update-consumer-v1}",
        containerFactory = "leaderboardKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String payload = record.value();
        if (payload == null || payload.isBlank()) {
            meterRegistry.counter("leaderboard.events.invalid_or_unsupported").increment();
            logWarn("unknown", "unknown", "unknown", "empty_payload");
            acknowledgment.acknowledge();
            return;
        }
        if (payload.getBytes(StandardCharsets.UTF_8).length > properties.getMaxPayloadBytes()) {
            meterRegistry.counter("leaderboard.events.invalid_or_unsupported").increment();
            logWarn("unknown", "unknown", "unknown", "payload_too_large");
            acknowledgment.acknowledge();
            return;
        }

        String safeEventId = "unknown";
        String safeEventType = "unknown";
        String safePlayerId = "unknown";
        try {
            PlayerActivityEventEnvelope envelope = parseEnvelope(payload);
            safeEventId = sanitizeForLog(envelope.eventId());
            safeEventType = sanitizeForLog(envelope.eventType());
            safePlayerId = sanitizeForLog(envelope.playerId());
            LeaderboardProcessingOutcome outcome =
                leaderboardScoreUpdateService.processEvent(envelope, properties.getGroupId());
            if (outcome == LeaderboardProcessingOutcome.DUPLICATE) {
                meterRegistry.counter("leaderboard.events.duplicate").increment();
            } else {
                meterRegistry.counter("leaderboard.events.processed").increment();
            }
            acknowledgment.acknowledge();
        } catch (InvalidLeaderboardEventException invalidEvent) {
            meterRegistry.counter("leaderboard.events.invalid_or_unsupported").increment();
            logWarn(safeEventId, safeEventType, safePlayerId, sanitizeForLog(invalidEvent.getMessage()));
            acknowledgment.acknowledge();
        } catch (DuplicateLeaderboardEventException duplicate) {
            meterRegistry.counter("leaderboard.events.duplicate").increment();
            acknowledgment.acknowledge();
        }
    }

    private PlayerActivityEventEnvelope parseEnvelope(String payload) {
        try {
            return objectMapper.readValue(payload, PlayerActivityEventEnvelope.class);
        } catch (JsonProcessingException malformedPayload) {
            throw new InvalidLeaderboardEventException("malformed_json");
        }
    }

    private void logWarn(String eventId, String eventType, String playerId, String reason) {
        log.warn(
            "leaderboard_consumer_invalid_or_unsupported eventId={} eventType={} playerId={} reason={}",
            eventId,
            eventType,
            playerId,
            reason
        );
    }

    private String sanitizeForLog(String value) {
        if (value == null) {
            return "unknown";
        }
        String sanitized = value
            .replace('\n', '_')
            .replace('\r', '_')
            .replace('\t', '_');
        if (sanitized.isBlank()) {
            return "unknown";
        }
        return sanitized.length() > 128 ? sanitized.substring(0, 128) : sanitized;
    }
}
