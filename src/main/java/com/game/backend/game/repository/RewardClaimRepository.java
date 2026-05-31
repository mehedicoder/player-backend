package com.game.backend.game.repository;

import com.game.backend.game.domain.RewardClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for reward claim records.
 */
public interface RewardClaimRepository extends JpaRepository<RewardClaim, Long> {
    Optional<RewardClaim> findByPlayerIdAndRewardId(String playerId, String rewardId);
    long countByPlayerId(String playerId);
}
