CREATE TABLE IF NOT EXISTS player_wallet (
    player_id VARCHAR(64) PRIMARY KEY,
    balance BIGINT NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_player_wallet_player FOREIGN KEY (player_id) REFERENCES player_profile (player_id)
);

CREATE TABLE IF NOT EXISTS wallet_ledger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id VARCHAR(64) NOT NULL,
    mutation_type VARCHAR(16) NOT NULL,
    amount BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    reason VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_wallet_ledger_player FOREIGN KEY (player_id) REFERENCES player_profile (player_id)
);

CREATE INDEX idx_wallet_ledger_player_created_at ON wallet_ledger (player_id, created_at DESC);

CREATE TABLE IF NOT EXISTS wallet_idempotency (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    ledger_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    mutation_type VARCHAR(16) NOT NULL,
    balance_after BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_wallet_idempotency UNIQUE (player_id, idempotency_key),
    CONSTRAINT fk_wallet_idempotency_ledger FOREIGN KEY (ledger_id) REFERENCES wallet_ledger (id)
);

CREATE INDEX idx_wallet_idempotency_player_created_at ON wallet_idempotency (player_id, created_at DESC);

CREATE TABLE IF NOT EXISTS reward_claim (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id VARCHAR(64) NOT NULL,
    reward_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    reward_amount BIGINT NOT NULL,
    ledger_id BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_reward_claim_player_reward UNIQUE (player_id, reward_id),
    CONSTRAINT uk_reward_claim_player_idempotency UNIQUE (player_id, idempotency_key),
    CONSTRAINT fk_reward_claim_ledger FOREIGN KEY (ledger_id) REFERENCES wallet_ledger (id)
);

CREATE INDEX idx_reward_claim_player_created_at ON reward_claim (player_id, created_at DESC);

CREATE TABLE IF NOT EXISTS player_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id VARCHAR(64) NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    quantity BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_player_inventory UNIQUE (player_id, item_code),
    CONSTRAINT fk_player_inventory_player FOREIGN KEY (player_id) REFERENCES player_profile (player_id)
);

CREATE INDEX idx_player_inventory_player ON player_inventory (player_id);
