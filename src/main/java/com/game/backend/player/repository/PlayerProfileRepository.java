package com.game.backend.player.repository;

import com.game.backend.player.domain.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence repository for {@link PlayerProfile}.
 */
public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, String> {
}
