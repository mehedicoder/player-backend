package com.game.backend.game.service;

/**
 * Raised when a wallet debit would produce a negative balance.
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String playerId, long balance, long amount) {
        super("Insufficient wallet balance for playerId=" + playerId + ", balance=" + balance + ", amount=" + amount);
    }
}

