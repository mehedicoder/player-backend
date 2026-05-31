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
 * Persisted inventory mutation event identity for stable downstream event dedupe.
 */
@Entity
@Table(name = "inventory_mutation_event")
public class InventoryMutationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, length = 64)
    private String playerId;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @Column(name = "item_code", nullable = false, length = 64)
    private String itemCode;

    @Column(name = "operation", nullable = false, length = 16)
    private String operation;

    @Column(name = "quantity_delta", nullable = false)
    private Long quantityDelta;

    @Column(name = "quantity_after", nullable = false)
    private Long quantityAfter;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public Long getInventoryItemId() { return inventoryItemId; }
    public void setInventoryItemId(Long inventoryItemId) { this.inventoryItemId = inventoryItemId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public Long getQuantityDelta() { return quantityDelta; }
    public void setQuantityDelta(Long quantityDelta) { this.quantityDelta = quantityDelta; }
    public Long getQuantityAfter() { return quantityAfter; }
    public void setQuantityAfter(Long quantityAfter) { this.quantityAfter = quantityAfter; }
}
