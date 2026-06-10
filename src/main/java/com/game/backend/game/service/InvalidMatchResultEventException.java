package com.game.backend.game.service;

/**
 * Raised for deterministic invalid match-result records that must not be retried.
 */
public class InvalidMatchResultEventException extends RuntimeException {

    public InvalidMatchResultEventException(String message) {
        super(message);
    }
}
