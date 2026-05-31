package com.game.backend.game.service;

import com.game.backend.game.api.WalletMutationRequest;
import com.game.backend.game.api.WalletMutationType;
import com.game.backend.game.domain.PlayerWallet;
import com.game.backend.game.domain.WalletIdempotencyRecord;
import com.game.backend.game.domain.WalletLedgerEntry;
import com.game.backend.game.repository.PlayerWalletRepository;
import com.game.backend.game.repository.WalletIdempotencyRepository;
import com.game.backend.game.repository.WalletLedgerRepository;
import com.game.backend.player.repository.PlayerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Test
    void mutateWallet_successfulDebit_publishesWalletDebitedEvent() {
        PlayerWallet wallet = new PlayerWallet();
        wallet.setPlayerId("p1");
        wallet.setBalance(100L);
        WalletMutationRequest request = new WalletMutationRequest(WalletMutationType.DEBIT, 20, "k3", "buy-item");
        when(playerRepository.existsById("p1")).thenReturn(true);
        when(idempotencyRepository.findByPlayerIdAndIdempotencyKey("p1", "k3")).thenReturn(Optional.empty());
        when(walletRepository.findByPlayerIdForUpdate("p1")).thenReturn(Optional.of(wallet));

        WalletLedgerEntry persistedLedger = new WalletLedgerEntry();
        ReflectionTestUtils.setField(persistedLedger, "id", 77L);
        when(ledgerRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(persistedLedger);

        walletService.mutateWallet("p1", request);

        verify(activityPublisher, times(1)).publishAfterCommit(
            org.mockito.ArgumentMatchers.eq(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1),
            org.mockito.ArgumentMatchers.eq("p1"),
            argThat(event ->
                event.eventType().equals(PlayerActivityEventType.WALLET_DEBITED_V1)
                    && event.eventId().equals("wallet-ledger-77")
                    && event.idempotencyKey().equals("k3")
                    && event.payload().get("reason").equals("BUY_ITEM")
                    && event.payload().get("balanceAfter").equals(80L)
            )
        );
    }
}
