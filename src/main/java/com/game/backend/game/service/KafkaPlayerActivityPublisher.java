package com.game.backend.game.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Kafka-backed player activity publisher.
 */
@Component
public class KafkaPlayerActivityPublisher implements PlayerActivityPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPlayerActivityPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaPlayerActivityPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes activity payload to Kafka.
     */
    @Override
    public void publish(String topic, String playerId, String action, String details) {
        kafkaTemplate.send(topic, playerId, Map.of(
            "playerId", playerId,
            "action", action,
            "details", details,
            "at", OffsetDateTime.now().toString()
        )).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish player activity event topic={} playerId={} action={}", topic, playerId, action, ex);
            }
        });
    }
}
