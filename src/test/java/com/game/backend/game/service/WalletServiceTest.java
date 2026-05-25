package com.game.backend.game.service;

import com.game.backend.game.api.WalletMutationRequest;
import com.game.backend.game.api.WalletMutationType;
import com.game.backend.game.domain.PlayerWallet;
import com.game.backend.game.domain.WalletIdempotencyRecord;
import com.game.backend.game.repository.PlayerWalletRepository;
import com.game.backend.game.repository.WalletIdempotencyRepository;
import com.game.backend.game.repository.WalletLedgerRepository;
import com.game.backend.player.repository.PlayerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private PlayerWalletRepository walletRepository;
    @Mock
    private WalletLedgerRepository ledgerRepository;
    @Mock
    private WalletIdempotencyRepository idempotencyRepository;
    @Mock
    private PlayerProfileRepository playerRepository;
    @Mock
    private TransactionalActivityPublisher activityPublisher;

    @InjectMocks
    private WalletService walletService;

    @Test
    void mutateWallet_duplicateIdempotency_returnsPersistedResultWithoutNewWrite() {
        WalletIdempotencyRecord existing = new WalletIdempotencyRecord();
        existing.setBalanceAfter(120L);
        when(playerRepository.existsById("p1")).thenReturn(true);
        when(walletRepository.findByPlayerIdForUpdate("p1")).thenReturn(Optional.of(new PlayerWallet()));
        when(idempotencyRepository.findByPlayerIdAndIdempotencyKey("p1", "k1")).thenReturn(Optional.of(existing));

        var response = walletService.mutateWallet("p1", new WalletMutationRequest(WalletMutationType.CREDIT, 100, "k1", "quest"));

        assertThat(response.balance()).isEqualTo(120L);
        verify(ledgerRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void mutateWallet_debitWithInsufficientBalance_throwsConflictException() {
        PlayerWallet wallet = new PlayerWallet();
        wallet.setPlayerId("p1");
        wallet.setBalance(5L);
        when(playerRepository.existsById("p1")).thenReturn(true);
        when(idempotencyRepository.findByPlayerIdAndIdempotencyKey("p1", "k2")).thenReturn(Optional.empty());
        when(walletRepository.findByPlayerIdForUpdate("p1")).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.mutateWallet("p1",
            new WalletMutationRequest(WalletMutationType.DEBIT, 10, "k2", "buy-item")))
            .isInstanceOf(InsufficientBalanceException.class);
    }
}
