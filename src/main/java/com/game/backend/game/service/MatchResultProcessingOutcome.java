package com.game.backend.game.service;

/**
 * Outcome from idempotent match-result processing.
 */
public enum MatchResultProcessingOutcome {
    PROCESSED,
    DUPLICATE
}
