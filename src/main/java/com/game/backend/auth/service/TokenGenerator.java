package com.game.backend.auth.service;

/**
 * Generates opaque auth tokens.
 */
public interface TokenGenerator {
    /**
     * Generates a new token value.
     */
    String generate();
}
