package com.game.backend.auth.service;

/**
 * Exception for missing authorization token.
 */
public class AuthTokenMissingException extends RuntimeException {
    public AuthTokenMissingException(String message) {
        super(message);
    }
}
