package com.game.backend.game.service;

import com.game.backend.game.api.RewardClaimResponse;
import com.game.backend.game.domain.PlayerWallet;
import com.game.backend.game.domain.RewardClaim;
import com.game.backend.game.domain.WalletLedgerEntry;
import com.game.backend.game.repository.PlayerWalletRepository;
import com.game.backend.game.repository.RewardClaimRepository;
import com.game.backend.game.repository.WalletLedgerRepository;
import com.game.backend.player.repository.PlayerProfileRepository;
import com.game.backend.player.service.PlayerNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles idempotent reward claim operations.
 */
@Service
public class RewardService {

    private final RewardClaimRepository rewardClaimRepository;
    private final PlayerWalletRepository walletRepository;
    private final WalletLedgerRepository ledgerRepository;
    private final PlayerProfileRepository playerRepository;
    private final TransactionalActivityPublisher activityPublisher;

    public RewardService(
        RewardClaimRepository rewardClaimRepository,
        PlayerWalletRepository walletRepository,
        WalletLedgerRepository ledgerRepository,
        PlayerProfileRepository playerRepository,
        TransactionalActivityPublisher activityPublisher
    ) {
        this.rewardClaimRepository = rewardClaimRepository;
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.playerRepository = playerRepository;
        this.activityPublisher = activityPublisher;
    }

    /**
     * Claims reward once per player+reward and returns persisted result for duplicates.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RewardClaimResponse claim(String playerId, String rewardId, String idempotencyKey, long rewardAmount) {
        assertPlayerExists(playerId);
        walletRepository.ensureWalletRow(playerId);
        PlayerWallet wallet = walletRepository.findByPlayerIdForUpdate(playerId).orElseThrow();
        RewardClaim existing = rewardClaimRepository.findByPlayerIdAndRewardId(playerId, rewardId).orElse(null);
        if (existing != null) {
            return new RewardClaimResponse(playerId, rewardId, existing.getRewardAmount(), existing.getBalanceAfter());
        }

        long nextBalance = wallet.getBalance() + rewardAmount;
        wallet.setBalance(nextBalance);
        walletRepository.save(wallet);

        WalletLedgerEntry ledger = new WalletLedgerEntry();
        ledger.setPlayerId(playerId);
        ledger.setMutationType("CREDIT");
        ledger.setAmount(rewardAmount);
        ledger.setBalanceAfter(nextBalance);
        ledger.setReason("REWARD_CLAIM:" + rewardId);
        WalletLedgerEntry persistedLedger = ledgerRepository.save(ledger);

        RewardClaim claim = new RewardClaim();
        claim.setPlayerId(playerId);
        claim.setRewardId(rewardId);
        claim.setIdempotencyKey(idempotencyKey);
        claim.setRewardAmount(rewardAmount);
        claim.setLedgerId(persistedLedger.getId());
        claim.setBalanceAfter(nextBalance);
        rewardClaimRepository.save(claim);

        activityPublisher.publishAfterCommit("player.reward.activity.v1", playerId, "REWARD_CLAIM", rewardId);
        return new RewardClaimResponse(playerId, rewardId, rewardAmount, nextBalance);
    }

    private void assertPlayerExists(String playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }
    }
}
