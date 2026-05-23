CREATE TABLE IF NOT EXISTS player_profile (
    player_id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(50) NOT NULL,
    email VARCHAR(190) NOT NULL,
    country_code CHAR(2) NOT NULL,
    level INT NOT NULL DEFAULT 1,
    experience_points BIGINT NOT NULL DEFAULT 0,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_player_profile_email UNIQUE (email)
);

CREATE INDEX idx_player_profile_updated_at ON player_profile (updated_at);
