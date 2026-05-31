package com.game.backend.game.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Deduplicated reward claim record.
 */
@Entity
@Table(name = "reward_claim")
public class RewardClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, length = 64)
    private String playerId;

    @Column(name = "reward_id", nullable = false, length = 64)
    private String rewardId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "reward_amount", nullable = false)
    private Long rewardAmount;

    @Column(name = "ledger_id", nullable = false)
    private Long ledgerId;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getRewardId() { return rewardId; }
    public void setRewardId(String rewardId) { this.rewardId = rewardId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Long getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(Long rewardAmount) { this.rewardAmount = rewardAmount; }
    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public Long getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Long balanceAfter) { this.balanceAfter = balanceAfter; }
}

