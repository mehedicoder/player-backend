package com.game.backend.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.backend.game.domain.NotificationJob;
import com.game.backend.game.repository.MatchResultProcessedEventRepository;
import com.game.backend.game.repository.MatchResultProjectionRepository;
import com.game.backend.game.repository.NotificationJobRepository;
import com.game.backend.player.repository.PlayerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchResultIngestionServiceTest {

    @Mock
    private MatchResultProjectionRepository projectionRepository;
    @Mock
    private MatchResultProcessedEventRepository processedEventRepository;
    @Mock
    private NotificationJobRepository notificationJobRepository;
    @Mock
    private PlayerProfileRepository playerProfileRepository;

    private MatchResultIngestionService service;

    @BeforeEach
    void setUp() {
        service = new MatchResultIngestionService(
            projectionRepository,
            processedEventRepository,
            notificationJobRepository,
            playerProfileRepository,
            new MatchResultConsumerProperties(),
            new ObjectMapper()
        );
        when(playerProfileRepository.existsById("player-1")).thenReturn(true);
    }

    @Test
    void processEvent_validEvent_persistsProjectionDedupeAndNotificationJob() {
        when(processedEventRepository.existsByConsumerGroupAndEventId("group-a", "event-1")).thenReturn(false);
        when(processedEventRepository.insertIgnoreProcessedEvent("group-a", "event-1", MatchResultEventType.MATCH_RESULT_RECORDED_V1, "player-1"))
            .thenReturn(1);
        when(projectionRepository.upsertIfFresh(
            "player-1",
            "match-1",
            "WIN",
            25,
            "event-1",
            java.time.OffsetDateTime.parse("2026-06-08T12:00:00Z")
        )).thenReturn(1);
        MatchResultEventEnvelope envelope = validEnvelope("event-1");

        MatchResultProcessingOutcome outcome = service.processEvent(envelope, "group-a");

        assertThat(outcome).isEqualTo(MatchResultProcessingOutcome.PROCESSED);
        verify(processedEventRepository).insertIgnoreProcessedEvent(
            "group-a",
            "event-1",
            MatchResultEventType.MATCH_RESULT_RECORDED_V1,
            "player-1"
        );
        verify(projectionRepository).upsertIfFresh(
            "player-1",
            "match-1",
            "WIN",
            25,
            "event-1",
            java.time.OffsetDateTime.parse("2026-06-08T12:00:00Z")
        );
        ArgumentCaptor<NotificationJob> job = ArgumentCaptor.forClass(NotificationJob.class);
        verify(notificationJobRepository).save(job.capture());
        assertThat(job.getValue().getEventId()).isEqualTo("event-1");
        assertThat(job.getValue().getPayloadJson()).contains("match-1");
    }

    @Test
    void processEvent_duplicateEvent_returnsDuplicateWithoutSideEffects() {
        when(processedEventRepository.existsByConsumerGroupAndEventId("group-a", "event-1")).thenReturn(true);

        MatchResultProcessingOutcome outcome = service.processEvent(validEnvelope("event-1"), "group-a");

        assertThat(outcome).isEqualTo(MatchResultProcessingOutcome.DUPLICATE);
        verify(processedEventRepository, never()).insertIgnoreProcessedEvent(any(), any(), any(), any());
        verify(projectionRepository, never()).upsertIfFresh(any(), any(), any(), anyInt(), any(), any());
        verify(notificationJobRepository, never()).save(any());
    }

    @Test
    void processEvent_duplicateInsertRace_returnsDuplicateWithoutSideEffects() {
        when(processedEventRepository.existsByConsumerGroupAndEventId("group-a", "event-1"))
            .thenReturn(false)
            .thenReturn(true);
        when(processedEventRepository.insertIgnoreProcessedEvent("group-a", "event-1", MatchResultEventType.MATCH_RESULT_RECORDED_V1, "player-1"))
            .thenReturn(0);

        MatchResultProcessingOutcome outcome = service.processEvent(validEnvelope("event-1"), "group-a");

        assertThat(outcome).isEqualTo(MatchResultProcessingOutcome.DUPLICATE);
        verify(projectionRepository, never()).upsertIfFresh(any(), any(), any(), anyInt(), any(), any());
        verify(notificationJobRepository, never()).save(any());
    }

    @Test
    void processEvent_staleProjection_doesNotNotify() {
        when(processedEventRepository.existsByConsumerGroupAndEventId("group-a", "event-1")).thenReturn(false);
        when(processedEventRepository.insertIgnoreProcessedEvent("group-a", "event-1", MatchResultEventType.MATCH_RESULT_RECORDED_V1, "player-1"))
            .thenReturn(1);
        when(projectionRepository.upsertIfFresh(
            "player-1",
            "match-1",
            "WIN",
            25,
            "event-1",
            java.time.OffsetDateTime.parse("2026-06-08T12:00:00Z")
        )).thenReturn(0);

        MatchResultProcessingOutcome outcome = service.processEvent(validEnvelope("event-1"), "group-a");

        assertThat(outcome).isEqualTo(MatchResultProcessingOutcome.PROCESSED);
        verify(notificationJobRepository, never()).save(any());
    }

    @Test
    void processEvent_missingPlayer_throwsInvalidEvent() {
        when(playerProfileRepository.existsById("player-1")).thenReturn(false);

        assertThatThrownBy(() -> service.processEvent(validEnvelope("event-1"), "group-a"))
            .isInstanceOf(InvalidMatchResultEventException.class)
            .hasMessageContaining("player");
        verify(processedEventRepository, never()).insertIgnoreProcessedEvent(any(), any(), any(), any());
    }

    @Test
    void processEvent_projectionUpsertRace_usesAtomicUpsertWithoutInvalidClassification() {
        when(processedEventRepository.existsByConsumerGroupAndEventId("group-a", "event-1")).thenReturn(false);
        when(processedEventRepository.insertIgnoreProcessedEvent("group-a", "event-1", MatchResultEventType.MATCH_RESULT_RECORDED_V1, "player-1"))
            .thenReturn(1);
        when(projectionRepository.upsertIfFresh(
            "player-1",
            "match-1",
            "WIN",
            25,
            "event-1",
            java.time.OffsetDateTime.parse("2026-06-08T12:00:00Z")
        )).thenReturn(2);

        MatchResultProcessingOutcome outcome = service.processEvent(validEnvelope("event-1"), "group-a");

        assertThat(outcome).isEqualTo(MatchResultProcessingOutcome.PROCESSED);
        verify(notificationJobRepository).save(any());
    }

    @Test
    void processEvent_invalidScoreDelta_throwsNonRetriableException() {
        MatchResultEventEnvelope envelope = new MatchResultEventEnvelope(
            "event-2",
            MatchResultEventType.MATCH_RESULT_RECORDED_V1,
            "player-1",
            "2026-06-08T12:00:00Z",
            Map.of("matchId", "match-1", "result", "WIN", "scoreDelta", 1001)
        );

        assertThatThrownBy(() -> service.processEvent(envelope, "group-a"))
            .isInstanceOf(InvalidMatchResultEventException.class)
            .hasMessageContaining("scoreDelta");
    }

    private MatchResultEventEnvelope validEnvelope(String eventId) {
        return new MatchResultEventEnvelope(
            eventId,
            MatchResultEventType.MATCH_RESULT_RECORDED_V1,
            "player-1",
            "2026-06-08T12:00:00Z",
            Map.of("matchId", "match-1", "result", "WIN", "scoreDelta", 25)
        );
    }
}
