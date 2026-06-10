package com.game.backend.game.service;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration for durable notification worker behavior.
 */
@Validated
@ConfigurationProperties(prefix = "notification.worker")
public class NotificationWorkerProperties {

    private boolean enabled = false;

    @Min(1)
    private int batchSize = 10;

    @Min(1)
    private int maxAttempts = 4;

    @Min(1)
    private long fixedDelayMs = 1000L;

    @Min(1)
    private long processingTimeoutSeconds = 300L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public long getProcessingTimeoutSeconds() {
        return processingTimeoutSeconds;
    }

    public void setProcessingTimeoutSeconds(long processingTimeoutSeconds) {
        this.processingTimeoutSeconds = processingTimeoutSeconds;
    }
}
