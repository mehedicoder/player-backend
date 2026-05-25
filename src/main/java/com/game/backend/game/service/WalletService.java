package com.game.backend.game.service;

import com.game.backend.game.api.WalletMutationRequest;
import com.game.backend.game.api.WalletMutationType;
import com.game.backend.game.api.WalletResponse;
import com.game.backend.game.domain.PlayerWallet;
import com.game.backend.game.domain.WalletIdempotencyRecord;
import com.game.backend.game.domain.WalletLedgerEntry;
import com.game.backend.game.repository.PlayerWalletRepository;
import com.game.backend.game.repository.WalletIdempotencyRepository;
import com.game.backend.game.repository.WalletLedgerRepository;
import com.game.backend.player.repository.PlayerProfileRepository;
import com.game.backend.player.service.PlayerNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles transactional wallet reads and mutations.
 */
@Service
public class WalletService {

    private final PlayerWalletRepository walletRepository;
    private final WalletLedgerRepository ledgerRepository;
    private final WalletIdempotencyRepository idempotencyRepository;
    private final PlayerProfileRepository playerRepository;
    private final TransactionalActivityPublisher activityPublisher;

    public WalletService(
        PlayerWalletRepository walletRepository,
        WalletLedgerRepository ledgerRepository,
        WalletIdempotencyRepository idempotencyRepository,
        PlayerProfileRepository playerRepository,
        TransactionalActivityPublisher activityPublisher
    ) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.playerRepository = playerRepository;
        this.activityPublisher = activityPublisher;
    }

    /**
     * Returns current wallet balance.
     */
    @Transactional
    public WalletResponse getWallet(String playerId) {
        assertPlayerExists(playerId);
        walletRepository.ensureWalletRow(playerId);
        PlayerWallet wallet = walletRepository.findById(playerId).orElseThrow();
        return new WalletResponse(playerId, wallet.getBalance(), wallet.getUpdatedAt());
    }

    /**
     * Applies idempotent credit/debit mutation and appends ledger.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletResponse mutateWallet(String playerId, WalletMutationRequest request) {
        assertPlayerExists(playerId);
        walletRepository.ensureWalletRow(playerId);
        PlayerWallet wallet = walletRepository.findByPlayerIdForUpdate(playerId).orElseThrow();
        WalletIdempotencyRecord dedupe = idempotencyRepository
            .findByPlayerIdAndIdempotencyKey(playerId, request.idempotencyKey())
            .orElse(null);
        if (dedupe != null) {
            return new WalletResponse(playerId, dedupe.getBalanceAfter(), wallet.getUpdatedAt());
        }

        long signedAmount = request.mutationType() == WalletMutationType.CREDIT ? request.amount() : -request.amount();
        long nextBalance = wallet.getBalance() + signedAmount;
        if (nextBalance < 0) {
            throw new InsufficientBalanceException(playerId, wallet.getBalance(), request.amount());
        }
        wallet.setBalance(nextBalance);
        walletRepository.save(wallet);

        WalletLedgerEntry ledger = new WalletLedgerEntry();
        ledger.setPlayerId(playerId);
        ledger.setMutationType(request.mutationType().name());
        ledger.setAmount(request.amount());
        ledger.setBalanceAfter(nextBalance);
        ledger.setReason(request.reason());
        WalletLedgerEntry persistedLedger = ledgerRepository.save(ledger);

        WalletIdempotencyRecord record = new WalletIdempotencyRecord();
        record.setPlayerId(playerId);
        record.setIdempotencyKey(request.idempotencyKey());
        record.setLedgerId(persistedLedger.getId());
        record.setAmount(request.amount());
        record.setMutationType(request.mutationType().name());
        record.setBalanceAfter(nextBalance);
        idempotencyRepository.save(record);

        activityPublisher.publishAfterCommit("player.wallet.activity.v1", playerId, "WALLET_" + request.mutationType(), request.reason());
        return new WalletResponse(playerId, nextBalance, wallet.getUpdatedAt());
    }

    private void assertPlayerExists(String playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }
    }
}
