package com.game.backend.auth.service;

/**
 * Exception for auth infrastructure failures such as Redis access issues.
 */
public class AuthInfrastructureException extends RuntimeException {
    public AuthInfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
