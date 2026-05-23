package com.game.backend.player.repository;

import com.game.backend.player.domain.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, String> {
}
