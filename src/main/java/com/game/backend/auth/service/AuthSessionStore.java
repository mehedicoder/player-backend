package com.game.backend.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed session store for auth tokens.
 */
@Component
public class AuthSessionStore {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionStore.class);
    private static final String SESSION_KEY_PREFIX = "auth:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration sessionTtl;

    public AuthSessionStore(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        @Value("${auth.session.ttl:PT30M}") Duration sessionTtl
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sessionTtl = sessionTtl;
    }

    /**
     * Returns configured session TTL duration.
     */
    public Duration sessionTtl() {
        return sessionTtl;
    }

    /**
     * Persists a session token payload in Redis.
     */
    public void save(SessionData sessionData) {
        try {
            redisTemplate.opsForValue().set(key(sessionData.token()), objectMapper.writeValueAsString(sessionData), sessionTtl);
        } catch (Exception e) {
            throw new AuthInfrastructureException("Failed to persist auth session", e);
        }
    }

    /**
     * Reads a session payload by token.
     */
    public Optional<SessionData> get(String token) {
        try {
            String payload = redisTemplate.opsForValue().get(key(token));
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, SessionData.class));
        } catch (Exception e) {
            log.warn("Failed to read auth session from Redis", e);
            throw new AuthInfrastructureException("Failed to read auth session", e);
        }
    }

    /**
     * Deletes a session token from Redis.
     */
    public void delete(String token) {
        try {
            redisTemplate.delete(key(token));
        } catch (Exception e) {
            throw new AuthInfrastructureException("Failed to delete auth session", e);
        }
    }

    private String key(String token) {
        return SESSION_KEY_PREFIX + token;
    }
}
