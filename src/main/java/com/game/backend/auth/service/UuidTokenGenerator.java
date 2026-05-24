package com.game.backend.auth.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * UUID-based token generator.
 */
@Component
public class UuidTokenGenerator implements TokenGenerator {
    /**
     * Generates a random UUID token string.
     */
    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
