package com.game.backend.auth.service;

import org.springframework.stereotype.Component;

/**
 * Parses and validates Authorization header values.
 */
@Component
public class AuthHeaderParser {

    /**
     * Extracts bearer token from Authorization header.
     */
    public String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new AuthTokenMissingException("Authorization header is required");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new AuthTokenInvalidException("Authorization header must use Bearer token");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new AuthTokenInvalidException("Bearer token is empty");
        }
        return token;
    }
}
