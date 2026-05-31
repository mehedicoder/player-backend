package com.game.backend.game.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerActivityEventEnvelopeTest {

    @Test
    void v1_createsExpectedEnvelopeFields() {
        PlayerActivityEventEnvelope envelope = PlayerActivityEventEnvelope.v1(
            "event-1",
            PlayerActivityEventType.WALLET_CREDITED_V1,
            "player-1",
            "corr-1",
            "idem-1",
            Map.of("walletId", "player-1", "amount", 10L)
        );

        assertThat(envelope.eventId()).isEqualTo("event-1");
        assertThat(envelope.eventType()).isEqualTo(PlayerActivityEventType.WALLET_CREDITED_V1);
        assertThat(envelope.schemaVersion()).isEqualTo(PlayerActivityEventEnvelope.SCHEMA_VERSION_V1);
        assertThat(envelope.playerId()).isEqualTo("player-1");
        assertThat(envelope.occurredAt()).isNotBlank();
        assertThat(envelope.source()).isEqualTo(PlayerActivityEventEnvelope.SOURCE);
        assertThat(envelope.correlationId()).isEqualTo("corr-1");
        assertThat(envelope.idempotencyKey()).isEqualTo("idem-1");
        assertThat(envelope.payload()).containsEntry("walletId", "player-1");
    }

    @Test
    void v1_withBlankEventId_throws() {
        assertThatThrownBy(() -> PlayerActivityEventEnvelope.v1(
            " ",
            PlayerActivityEventType.WALLET_CREDITED_V1,
            "player-1",
            null,
            null,
            Map.of("walletId", "player-1")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void v1_withNullPayload_throws() {
        assertThatThrownBy(() -> PlayerActivityEventEnvelope.v1(
            "event-1",
            PlayerActivityEventType.WALLET_CREDITED_V1,
            "player-1",
            null,
            null,
            null
        )).isInstanceOf(NullPointerException.class);
    }
}
