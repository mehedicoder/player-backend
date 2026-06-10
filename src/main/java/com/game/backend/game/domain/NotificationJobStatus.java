package com.game.backend.game.domain;

/**
 * Durable async notification job status.
 */
public enum NotificationJobStatus {
    PENDING,
    PROCESSING,
    RETRY_WAIT,
    SENT,
    FAILED_PERMANENT
}
