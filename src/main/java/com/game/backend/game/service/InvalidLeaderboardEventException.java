package com.game.backend.game.service;

/**
 * Indicates non-retriable leaderboard consumer input errors.
 */
public class InvalidLeaderboardEventException extends RuntimeException {

    public InvalidLeaderboardEventException(String message) {
        super(message);
    }
}
