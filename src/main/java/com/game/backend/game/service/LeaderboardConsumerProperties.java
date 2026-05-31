package com.game.backend.game.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration for leaderboard consumer behavior.
 */
@Validated
@ConfigurationProperties(prefix = "leaderboard.consumer")
public class LeaderboardConsumerProperties {

    @NotBlank
    private String groupId = "leaderboard-update-consumer-v1";

    @NotBlank
    private String topic = PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1;

    @Min(1)
    private int concurrency = 1;

    @Min(1)
    private int retryMaxAttempts = 3;

    @Min(1)
    private long retryInitialIntervalMs = 500L;

    @Min(1)
    private long retryMaxIntervalMs = 1000L;

    @Min(256)
    private int maxPayloadBytes = 65536;

    @Min(1)
    private long maxScoreDelta = 1_000_000L;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public void setRetryMaxAttempts(int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
    }

    public long getRetryInitialIntervalMs() {
        return retryInitialIntervalMs;
    }

    public void setRetryInitialIntervalMs(long retryInitialIntervalMs) {
        this.retryInitialIntervalMs = retryInitialIntervalMs;
    }

    public long getRetryMaxIntervalMs() {
        return retryMaxIntervalMs;
    }

    public void setRetryMaxIntervalMs(long retryMaxIntervalMs) {
        this.retryMaxIntervalMs = retryMaxIntervalMs;
    }

    public int getMaxPayloadBytes() {
        return maxPayloadBytes;
    }

    public void setMaxPayloadBytes(int maxPayloadBytes) {
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public long getMaxScoreDelta() {
        return maxScoreDelta;
    }

    public void setMaxScoreDelta(long maxScoreDelta) {
        this.maxScoreDelta = maxScoreDelta;
    }
}
