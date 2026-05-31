package com.game.backend.game.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionalActivityPublisherTest {

    @Mock
    private PlayerActivityPublisher publisher;

    @AfterEach
    void clearTxState() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishAfterCommit_withoutTransaction_throwsIllegalState() {
        TransactionalActivityPublisher transactionalPublisher = new TransactionalActivityPublisher(publisher);
        PlayerActivityEventEnvelope event = PlayerActivityEventEnvelope.v1(
            "e1", PlayerActivityEventType.WALLET_CREDITED_V1, "p1", null, "idem", Map.of("amount", 10L)
        );

        assertThatThrownBy(() ->
            transactionalPublisher.publishAfterCommit(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1, "p1", event)
        ).isInstanceOf(IllegalStateException.class);
        verify(publisher, never()).publish(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1, "p1", event);
    }

    @Test
    void publishAfterCommit_registersCallbackWhenTransactionActive() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionalActivityPublisher transactionalPublisher = new TransactionalActivityPublisher(publisher);
        PlayerActivityEventEnvelope event = PlayerActivityEventEnvelope.v1(
            "e2", PlayerActivityEventType.REWARD_CLAIMED_V1, "p2", null, "idem-2", Map.of("rewardId", "r1")
        );

        transactionalPublisher.publishAfterCommit(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1, "p2", event);
        verify(publisher, never()).publish(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1, "p2", event);

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);

        verify(publisher, times(1)).publish(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1, "p2", event);
    }
}
