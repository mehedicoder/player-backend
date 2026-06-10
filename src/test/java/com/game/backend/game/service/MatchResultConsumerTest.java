package com.game.backend.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchResultConsumerTest {

    private MatchResultIngestionService ingestionService;
    private MatchResultConsumer consumer;
    private Acknowledgment acknowledgment;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        ingestionService = mock(MatchResultIngestionService.class);
        acknowledgment = mock(Acknowledgment.class);
        meterRegistry = new SimpleMeterRegistry();
        consumer = new MatchResultConsumer(
            new ObjectMapper(),
            ingestionService,
            new MatchResultConsumerProperties(),
            meterRegistry
        );
    }

    @Test
    void consume_malformedJson_acknowledgesWithoutServiceCall() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "key", "{not-json");

        consumer.consume(record, acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(ingestionService, never()).processEvent(any(), anyString());
        assertThat(meterRegistry.counter("match_result.events.invalid_or_unsupported").count()).isEqualTo(1.0d);
    }

    @Test
    void consume_invalidEventException_acknowledges() throws Exception {
        when(ingestionService.processEvent(any(), anyString()))
            .thenThrow(new InvalidMatchResultEventException("bad_payload"));
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "topic",
            0,
            0L,
            "key",
            new ObjectMapper().writeValueAsString(validEnvelope())
        );

        consumer.consume(record, acknowledgment);

        verify(acknowledgment).acknowledge();
        assertThat(meterRegistry.counter("match_result.events.invalid_or_unsupported").count()).isEqualTo(1.0d);
    }

    @Test
    void consume_processedEvent_incrementsProcessedCounter() throws Exception {
        when(ingestionService.processEvent(any(), anyString())).thenReturn(MatchResultProcessingOutcome.PROCESSED);
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "topic",
            0,
            0L,
            "key",
            new ObjectMapper().writeValueAsString(validEnvelope())
        );

        consumer.consume(record, acknowledgment);

        verify(acknowledgment).acknowledge();
        assertThat(meterRegistry.counter("match_result.events.processed").count()).isEqualTo(1.0d);
    }

    @Test
    void consume_duplicateEvent_incrementsDuplicateCounter() throws Exception {
        when(ingestionService.processEvent(any(), anyString())).thenReturn(MatchResultProcessingOutcome.DUPLICATE);
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "topic",
            0,
            0L,
            "key",
            new ObjectMapper().writeValueAsString(validEnvelope())
        );

        consumer.consume(record, acknowledgment);

        verify(acknowledgment).acknowledge();
        assertThat(meterRegistry.counter("match_result.events.duplicate").count()).isEqualTo(1.0d);
    }

    @Test
    void consume_retriableFailure_rethrowsWithoutAck() throws Exception {
        when(ingestionService.processEvent(any(), anyString())).thenThrow(new RuntimeException("db_down"));
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "topic",
            0,
            0L,
            "key",
            new ObjectMapper().writeValueAsString(validEnvelope())
        );

        assertThatThrownBy(() -> consumer.consume(record, acknowledgment))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("db_down");
        verify(acknowledgment, never()).acknowledge();
    }

    private MatchResultEventEnvelope validEnvelope() {
        return new MatchResultEventEnvelope(
            "event-1",
            MatchResultEventType.MATCH_RESULT_RECORDED_V1,
            "player-1",
            "2026-06-08T12:00:00Z",
            Map.of("matchId", "match-1", "result", "WIN", "scoreDelta", 25)
        );
    }
}
