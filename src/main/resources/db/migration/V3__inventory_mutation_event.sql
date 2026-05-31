CREATE TABLE IF NOT EXISTS inventory_mutation_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id VARCHAR(64) NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    operation VARCHAR(16) NOT NULL,
    quantity_delta BIGINT NOT NULL,
    quantity_after BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_inventory_mutation_event_player FOREIGN KEY (player_id) REFERENCES player_profile (player_id),
    CONSTRAINT fk_inventory_mutation_event_item FOREIGN KEY (inventory_item_id) REFERENCES player_inventory (id)
);

CREATE INDEX idx_inventory_mutation_event_player_created_at
    ON inventory_mutation_event (player_id, created_at DESC);
