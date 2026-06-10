package com.game.backend.game.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled adapter for running the durable notification worker when enabled.
 */
@Component
public class NotificationWorkerScheduler {

    private final NotificationWorker notificationWorker;
    private final NotificationWorkerProperties properties;

    public NotificationWorkerScheduler(NotificationWorker notificationWorker, NotificationWorkerProperties properties) {
        this.notificationWorker = notificationWorker;
        this.properties = properties;
    }

    /**
     * Runs a bounded worker batch only when notification worker scheduling is enabled.
     */
    @Scheduled(fixedDelayString = "${notification.worker.fixed-delay-ms:1000}")
    public void runScheduledBatch() {
        if (properties.isEnabled()) {
            notificationWorker.processDueBatch();
        }
    }
}
