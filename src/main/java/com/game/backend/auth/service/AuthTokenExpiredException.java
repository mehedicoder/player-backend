package com.game.backend.auth.service;

/**
 * Exception for expired auth token usage.
 */
public class AuthTokenExpiredException extends RuntimeException {
    public AuthTokenExpiredException(String message) {
        super(message);
    }
}
