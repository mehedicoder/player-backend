package com.game.backend.game.service;

/**
 * Result category for leaderboard consumer event processing.
 */
public enum LeaderboardProcessingOutcome {
    PROCESSED,
    DUPLICATE
}
