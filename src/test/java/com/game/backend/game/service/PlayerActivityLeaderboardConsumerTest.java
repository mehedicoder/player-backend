package com.game.backend.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerActivityLeaderboardConsumerTest {

    private LeaderboardScoreUpdateService updateService;
    private PlayerActivityLeaderboardConsumer consumer;
    private Acknowledgment acknowledgment;

    @BeforeEach
    void setUp() {
        updateService = mock(LeaderboardScoreUpdateService.class);
        acknowledgment = mock(Acknowledgment.class);
        LeaderboardConsumerProperties properties = new LeaderboardConsumerProperties();
        consumer = new PlayerActivityLeaderboardConsumer(
            new ObjectMapper(),
            updateService,
            properties,
            new SimpleMeterRegistry()
        );
    }

    @Test
    void consume_invalidPayload_acknowledgesWithoutServiceCall() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "key", " ");

        consumer.consume(record, acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(updateService, never()).processEvent(any(), anyString());
    }

    @Test
    void consume_invalidEventException_acknowledges() throws Exception {
        PlayerActivityEventEnvelope envelope = PlayerActivityEventEnvelope.v1(
            "event-1",
            PlayerActivityEventType.WALLET_CREDITED_V1,
            "player-1",
            null,
            null,
            Map.of("amount", 5)
        );
        when(updateService.processEvent(any(), anyString()))
            .thenThrow(new InvalidLeaderboardEventException("bad_payload"));
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "topic",
            0,
            0L,
            "key",
            new ObjectMapper().writeValueAsString(envelope)
        );

        consumer.consume(record, acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void consume_retriableFailure_rethrowsWithoutAck() throws Exception {
        PlayerActivityEventEnvelope envelope = PlayerActivityEventEnvelope.v1(
            "event-2",
            PlayerActivityEventType.WALLET_CREDITED_V1,
            "player-1",
            null,
            null,
            Map.of("amount", 5)
        );
        when(updateService.processEvent(any(), anyString()))
            .thenThrow(new RuntimeException("db_down"));
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "topic",
            0,
            0L,
            "key",
            new ObjectMapper().writeValueAsString(envelope)
        );

        assertThatThrownBy(() -> consumer.consume(record, acknowledgment))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("db_down");
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consume_malformedJson_acknowledgesWithoutServiceCall() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "key", "{not-json");

        consumer.consume(record, acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(updateService, never()).processEvent(any(), anyString());
    }

    @Test
    void consume_nullPayloadInEnvelope_acknowledgesAsInvalidEvent() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "topic",
            0,
            0L,
            "key",
            "{\"eventId\":\"event-9\",\"eventType\":\"wallet.credited.v1\",\"schemaVersion\":\"v1\",\"playerId\":\"player-1\",\"occurredAt\":\"2026-05-31T00:00:00Z\",\"source\":\"player-backend\",\"correlationId\":null,\"idempotencyKey\":null,\"payload\":null}"
        );

        consumer.consume(record, acknowledgment);

        verify(acknowledgment).acknowledge();
    }
}
