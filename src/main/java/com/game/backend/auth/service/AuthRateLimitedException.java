package com.game.backend.auth.service;

/**
 * Exception raised when login attempt throttling is triggered.
 */
public class AuthRateLimitedException extends RuntimeException {
    public AuthRateLimitedException(String message) {
        super(message);
    }
}
