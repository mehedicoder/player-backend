package com.game.backend.player.service;

/**
 * Exception raised when a player profile cannot be found by identifier.
 */
public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String playerId) {
        super("Player not found: " + playerId);
    }
}
