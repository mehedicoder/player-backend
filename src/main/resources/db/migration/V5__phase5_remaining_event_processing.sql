CREATE TABLE IF NOT EXISTS match_result_projection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id VARCHAR(64) NOT NULL,
    match_id VARCHAR(128) NOT NULL,
    result VARCHAR(16) NOT NULL,
    score_delta INT NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_match_result_player_match UNIQUE (player_id, match_id),
    CONSTRAINT uk_match_result_event UNIQUE (event_id),
    CONSTRAINT fk_match_result_player FOREIGN KEY (player_id) REFERENCES player_profile (player_id)
);

CREATE INDEX idx_match_result_player_updated
    ON match_result_projection (player_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS match_result_processed_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    consumer_group VARCHAR(128) NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    player_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_match_result_processed_consumer_event UNIQUE (consumer_group, event_id),
    CONSTRAINT fk_match_result_processed_player FOREIGN KEY (player_id) REFERENCES player_profile (player_id)
);

CREATE INDEX idx_match_result_processed_player_created
    ON match_result_processed_event (player_id, created_at DESC);

CREATE TABLE IF NOT EXISTS notification_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(128) NOT NULL,
    player_id VARCHAR(64) NOT NULL,
    notification_type VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL,
    claim_token VARCHAR(64) NULL,
    last_error VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_notification_job_event UNIQUE (event_id),
    CONSTRAINT fk_notification_job_player FOREIGN KEY (player_id) REFERENCES player_profile (player_id)
);

CREATE INDEX idx_notification_job_status_next_attempt
    ON notification_job (status, next_attempt_at, id);

CREATE INDEX idx_notification_job_claim_token
    ON notification_job (claim_token);
