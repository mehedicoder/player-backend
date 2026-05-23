package com.game.backend.player.service;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String playerId) {
        super("Player not found: " + playerId);
    }
}
