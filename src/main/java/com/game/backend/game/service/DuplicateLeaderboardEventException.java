package com.game.backend.game.service;

/**
 * Indicates duplicate processed-event insertion for leaderboard consumer.
 */
public class DuplicateLeaderboardEventException extends RuntimeException {

    public DuplicateLeaderboardEventException(Throwable cause) {
        super(cause);
    }
}
