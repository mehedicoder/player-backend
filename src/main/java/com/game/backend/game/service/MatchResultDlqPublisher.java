package com.game.backend.game.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Publishes retry-exhausted match-result records to the configured DLQ topic.
 */
@Component
public class MatchResultDlqPublisher {

    private static final Logger log = LoggerFactory.getLogger(MatchResultDlqPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MatchResultConsumerProperties properties;
    private final MeterRegistry meterRegistry;

    public MatchResultDlqPublisher(
        KafkaTemplate<String, Object> kafkaTemplate,
        ObjectMapper objectMapper,
        MatchResultConsumerProperties properties,
        MeterRegistry meterRegistry
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Routes one retry-exhausted record to the DLQ with operational metadata.
     */
    public void publish(ConsumerRecord<?, ?> record, Exception exception) {
        Map<String, Object> dlqPayload = new LinkedHashMap<>();
        EventIdentity identity = extractIdentity(record.value());
        dlqPayload.put("originalTopic", record.topic());
        dlqPayload.put("originalPartition", record.partition());
        dlqPayload.put("originalOffset", record.offset());
        dlqPayload.put("eventId", identity.eventId());
        dlqPayload.put("eventType", identity.eventType());
        dlqPayload.put("consumerGroup", properties.getGroupId());
        dlqPayload.put("errorCategory", "TRANSIENT_RETRY_EXHAUSTED");
        dlqPayload.put("errorMessage", sanitize(exception.getClass().getSimpleName()));
        dlqPayload.put("failedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        try {
            kafkaTemplate.send(properties.getDlqTopic(), String.valueOf(record.key()), dlqPayload)
                .get(5, TimeUnit.SECONDS);
            meterRegistry.counter("match_result.events.dlq_routed").increment();
            log.error(
                "match_result_consumer_dlq_routed topic={} partition={} offset={} eventId={} eventType={} reason={}",
                record.topic(),
                record.partition(),
                record.offset(),
                identity.eventId(),
                identity.eventType(),
                exception.getClass().getSimpleName()
            );
        } catch (Exception publishFailure) {
            throw new IllegalStateException("failed to publish match-result DLQ record", publishFailure);
        }
    }

    private EventIdentity extractIdentity(Object rawPayload) {
        if (!(rawPayload instanceof String payload) || payload.isBlank()) {
            return new EventIdentity("unknown", "unknown");
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            return new EventIdentity(
                sanitize(root.path("eventId").asText("unknown")),
                sanitize(root.path("eventType").asText("unknown"))
            );
        } catch (Exception ignored) {
            return new EventIdentity("unknown", "unknown");
        }
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String sanitized = value.replace('\n', '_').replace('\r', '_').replace('\t', '_');
        return sanitized.length() > 512 ? sanitized.substring(0, 512) : sanitized;
    }

    private record EventIdentity(String eventId, String eventType) {
    }
}
