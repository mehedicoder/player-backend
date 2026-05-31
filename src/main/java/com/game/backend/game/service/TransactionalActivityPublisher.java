package com.game.backend.game.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Dispatches activity events only after transaction commit.
 */
@Component
public class TransactionalActivityPublisher {

    private final PlayerActivityPublisher publisher;

    public TransactionalActivityPublisher(PlayerActivityPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Publishes activity event after successful commit when a transaction exists.
     */
    public void publishAfterCommit(String topic, String key, PlayerActivityEventEnvelope event) {
        boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
        boolean syncActive = TransactionSynchronizationManager.isSynchronizationActive();
        if (!txActive || !syncActive) {
            throw new IllegalStateException("publishAfterCommit requires an active transaction and synchronization");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.publish(topic, key, event);
            }
        });
    }
}

