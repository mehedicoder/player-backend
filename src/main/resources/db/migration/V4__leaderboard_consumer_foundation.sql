CREATE TABLE IF NOT EXISTS leaderboard_score (
    player_id VARCHAR(64) PRIMARY KEY,
    score BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_leaderboard_score_player FOREIGN KEY (player_id) REFERENCES player_profile (player_id)
);

CREATE INDEX idx_leaderboard_score_score_updated
    ON leaderboard_score (score DESC, updated_at DESC);

CREATE TABLE IF NOT EXISTS leaderboard_processed_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    consumer_group VARCHAR(128) NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    player_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_leaderboard_processed_consumer_event UNIQUE (consumer_group, event_id),
    CONSTRAINT fk_leaderboard_processed_event_player FOREIGN KEY (player_id) REFERENCES player_profile (player_id)
);

CREATE INDEX idx_leaderboard_processed_player_created
    ON leaderboard_processed_event (player_id, created_at DESC);
