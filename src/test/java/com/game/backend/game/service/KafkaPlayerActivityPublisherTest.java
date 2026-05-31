package com.game.backend.game.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaPlayerActivityPublisherTest {

    @Mock
    private KafkaTemplate<String, PlayerActivityEventEnvelope> kafkaTemplate;

    @Test
    void publish_sendsEnvelopeWithTopicAndKey() {
        PlayerActivityEventEnvelope event = PlayerActivityEventEnvelope.v1(
            "event-1",
            PlayerActivityEventType.INVENTORY_MUTATED_V1,
            "player-1",
            null,
            null,
            Map.of("itemId", "WOOD")
        );
        when(kafkaTemplate.send(
            eq(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1),
            eq("player-1"),
            eq(event)
        )).thenReturn(CompletableFuture.<SendResult<String, PlayerActivityEventEnvelope>>completedFuture(null));

        KafkaPlayerActivityPublisher publisher = new KafkaPlayerActivityPublisher(kafkaTemplate);
        publisher.publish(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1, "player-1", event);

        verify(kafkaTemplate, times(1)).send(
            PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1,
            "player-1",
            event
        );
    }

    @Test
    void publish_whenPrimarySendFails_routesToDlq() {
        PlayerActivityEventEnvelope event = PlayerActivityEventEnvelope.v1(
            "event-2",
            PlayerActivityEventType.WALLET_DEBITED_V1,
            "player-2",
            null,
            "idem-2",
            Map.of("reason", "BUY_ITEM")
        );
        CompletableFuture<SendResult<String, PlayerActivityEventEnvelope>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaTemplate.send(
            eq(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1),
            eq("player-2"),
            eq(event)
        )).thenReturn(failed);
        when(kafkaTemplate.send(
            eq(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1_DLQ),
            eq("player-2"),
            eq(event)
        )).thenReturn(CompletableFuture.<SendResult<String, PlayerActivityEventEnvelope>>completedFuture(null));

        KafkaPlayerActivityPublisher publisher = new KafkaPlayerActivityPublisher(kafkaTemplate);
        publisher.publish(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1, "player-2", event);

        verify(kafkaTemplate, times(1)).send(
            PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1,
            "player-2",
            event
        );
        verify(kafkaTemplate, times(1)).send(
            PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1_DLQ,
            "player-2",
            event
        );
    }
}
