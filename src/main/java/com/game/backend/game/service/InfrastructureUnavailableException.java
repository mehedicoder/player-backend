package com.game.backend.game.service;

/**
 * Raised when infrastructure dependency cannot serve the request.
 */
public class InfrastructureUnavailableException extends RuntimeException {
    public InfrastructureUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

