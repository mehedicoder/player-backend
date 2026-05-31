package com.game.backend.game.service;

import com.game.backend.game.domain.LeaderboardScore;
import com.game.backend.game.repository.LeaderboardProcessedEventRepository;
import com.game.backend.game.repository.LeaderboardScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardScoreUpdateServiceTest {

    @Mock
    private LeaderboardScoreRepository leaderboardScoreRepository;
    @Mock
    private LeaderboardProcessedEventRepository processedEventRepository;

    private LeaderboardScoreUpdateService service;
    private LeaderboardConsumerProperties properties;

    @BeforeEach
    void setUp() {
        properties = new LeaderboardConsumerProperties();
        service = new LeaderboardScoreUpdateService(leaderboardScoreRepository, processedEventRepository, properties);
    }

    @Test
    void processEvent_duplicateEvent_returnsDuplicateWithoutMutation() {
        when(processedEventRepository.existsByConsumerGroupAndEventId("group-a", "event-1")).thenReturn(true);
        PlayerActivityEventEnvelope envelope = PlayerActivityEventEnvelope.v1(
            "event-1",
            PlayerActivityEventType.WALLET_CREDITED_V1,
            "player-1",
            null,
            null,
            Map.of("amount", 10)
        );

        LeaderboardProcessingOutcome outcome = service.processEvent(envelope, "group-a");

        assertThat(outcome).isEqualTo(LeaderboardProcessingOutcome.DUPLICATE);
        verify(processedEventRepository, never()).saveAndFlush(any());
        verify(leaderboardScoreRepository, never()).save(any());
    }

    @Test
    void processEvent_walletDebit_clampsScoreToZero() {
        when(processedEventRepository.existsByConsumerGroupAndEventId(anyString(), anyString())).thenReturn(false);
        LeaderboardScore existing = new LeaderboardScore();
        existing.setPlayerId("player-1");
        existing.setScore(5L);
        when(leaderboardScoreRepository.findByPlayerIdForUpdate("player-1")).thenReturn(Optional.of(existing));
        PlayerActivityEventEnvelope envelope = PlayerActivityEventEnvelope.v1(
            "event-2",
            PlayerActivityEventType.WALLET_DEBITED_V1,
            "player-1",
            null,
            null,
            Map.of("amount", 10)
        );

        LeaderboardProcessingOutcome outcome = service.processEvent(envelope, "group-a");

        assertThat(outcome).isEqualTo(LeaderboardProcessingOutcome.PROCESSED);
        assertThat(existing.getScore()).isEqualTo(0L);
        verify(leaderboardScoreRepository).save(existing);
    }

    @Test
    void processEvent_unsupportedType_throwsInvalidEvent() {
        PlayerActivityEventEnvelope envelope = PlayerActivityEventEnvelope.v1(
            "event-3",
            "unsupported.v1",
            "player-1",
            null,
            null,
            Map.of("amount", 10)
        );

        assertThatThrownBy(() -> service.processEvent(envelope, "group-a"))
            .isInstanceOf(InvalidLeaderboardEventException.class);
    }

    @Test
    void processEvent_nullPayload_throwsInvalidEvent() {
        PlayerActivityEventEnvelope envelope = new PlayerActivityEventEnvelope(
            "event-4",
            PlayerActivityEventType.WALLET_CREDITED_V1,
            PlayerActivityEventEnvelope.SCHEMA_VERSION_V1,
            "player-1",
            "2026-05-31T00:00:00Z",
            PlayerActivityEventEnvelope.SOURCE,
            null,
            null,
            null
        );

        assertThatThrownBy(() -> service.processEvent(envelope, "group-a"))
            .isInstanceOf(InvalidLeaderboardEventException.class)
            .hasMessageContaining("payload is null");
    }
}
