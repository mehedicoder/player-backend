package com.game.backend.auth.service;

/**
 * Exception for malformed or unknown auth tokens.
 */
public class AuthTokenInvalidException extends RuntimeException {
    public AuthTokenInvalidException(String message) {
        super(message);
    }
}
