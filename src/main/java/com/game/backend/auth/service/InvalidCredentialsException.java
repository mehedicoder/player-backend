package com.game.backend.auth.service;

/**
 * Exception for failed login credential validation.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
