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
 * Kafka adapter that consumes match-result events for idempotent ingestion.
 */
@Component
public class MatchResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(MatchResultConsumer.class);

    private final ObjectMapper objectMapper;
    private final MatchResultIngestionService ingestionService;
    private final MatchResultConsumerProperties properties;
    private final MeterRegistry meterRegistry;

    public MatchResultConsumer(
        ObjectMapper objectMapper,
        MatchResultIngestionService ingestionService,
        MatchResultConsumerProperties properties,
        MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.ingestionService = ingestionService;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Consumes one match-result event with explicit invalid-event ack behavior.
     */
    @KafkaListener(
        topics = "${match-result.consumer.topic:match-results.v1}",
        groupId = "${match-result.consumer.group-id:match-result-consumer-v1}",
        containerFactory = "matchResultKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String payload = record.value();
        if (payload == null || payload.isBlank()) {
            skipInvalid(acknowledgment, "unknown", "unknown", "unknown", "empty_payload");
            return;
        }
        if (payload.getBytes(StandardCharsets.UTF_8).length > properties.getMaxPayloadBytes()) {
            skipInvalid(acknowledgment, "unknown", "unknown", "unknown", "payload_too_large");
            return;
        }

        String safeEventId = "unknown";
        String safeEventType = "unknown";
        String safePlayerId = "unknown";
        try {
            MatchResultEventEnvelope envelope = parseEnvelope(payload);
            safeEventId = sanitizeForLog(envelope.eventId());
            safeEventType = sanitizeForLog(envelope.eventType());
            safePlayerId = sanitizeForLog(envelope.playerId());
            MatchResultProcessingOutcome outcome = ingestionService.processEvent(envelope, properties.getGroupId());
            if (outcome == MatchResultProcessingOutcome.DUPLICATE) {
                meterRegistry.counter("match_result.events.duplicate").increment();
            } else {
                meterRegistry.counter("match_result.events.processed").increment();
            }
            acknowledgment.acknowledge();
        } catch (InvalidMatchResultEventException invalidEvent) {
            skipInvalid(acknowledgment, safeEventId, safeEventType, safePlayerId, sanitizeForLog(invalidEvent.getMessage()));
        }
    }

    private MatchResultEventEnvelope parseEnvelope(String payload) {
        try {
            return objectMapper.readValue(payload, MatchResultEventEnvelope.class);
        } catch (JsonProcessingException malformedPayload) {
            throw new InvalidMatchResultEventException("malformed_json");
        }
    }

    private void skipInvalid(
        Acknowledgment acknowledgment,
        String eventId,
        String eventType,
        String playerId,
        String reason
    ) {
        meterRegistry.counter("match_result.events.invalid_or_unsupported").increment();
        log.warn(
            "match_result_consumer_invalid_or_unsupported eventId={} eventType={} playerId={} reason={}",
            eventId,
            eventType,
            playerId,
            reason
        );
        acknowledgment.acknowledge();
    }

    private String sanitizeForLog(String value) {
        if (value == null) {
            return "unknown";
        }
        String sanitized = value.replace('\n', '_').replace('\r', '_').replace('\t', '_');
        if (sanitized.isBlank()) {
            return "unknown";
        }
        return sanitized.length() > 128 ? sanitized.substring(0, 128) : sanitized;
    }
}
