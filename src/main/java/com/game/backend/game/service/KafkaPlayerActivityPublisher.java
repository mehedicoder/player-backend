package com.game.backend.game.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed player activity publisher.
 */
@Component
public class KafkaPlayerActivityPublisher implements PlayerActivityPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPlayerActivityPublisher.class);

    private final KafkaTemplate<String, PlayerActivityEventEnvelope> kafkaTemplate;

    public KafkaPlayerActivityPublisher(KafkaTemplate<String, PlayerActivityEventEnvelope> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes activity payload to Kafka.
     */
    @Override
    public void publish(String topic, String key, PlayerActivityEventEnvelope event) {
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                    "Failed to publish player activity event topic={} key={} eventType={} eventId={}",
                    topic,
                    key,
                    event.eventType(),
                    event.eventId(),
                    ex
                );
                kafkaTemplate.send(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1_DLQ, key, event)
                    .whenComplete((dlqResult, dlqEx) -> {
                        if (dlqEx != null) {
                            log.error(
                                "Failed to route player activity event to DLQ topic={} key={} eventType={} eventId={}",
                                PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1_DLQ,
                                key,
                                event.eventType(),
                                event.eventId(),
                                dlqEx
                            );
                        }
                    });
            }
        });
    }
}
