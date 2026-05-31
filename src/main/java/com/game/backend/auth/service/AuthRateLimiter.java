package com.game.backend.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed login-failure rate limiter.
 */
@Component
public class AuthRateLimiter {

    private static final String RATE_KEY_PREFIX = "auth:login:fail:";

    private final StringRedisTemplate redisTemplate;
    private final int maxAttempts;
    private final Duration window;

    public AuthRateLimiter(
        StringRedisTemplate redisTemplate,
        @Value("${auth.login-rate-limit.max-attempts:5}") int maxAttempts,
        @Value("${auth.login-rate-limit.window:PT5M}") Duration window
    ) {
        this.redisTemplate = redisTemplate;
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    /**
     * Checks whether the login key is currently throttled.
     */
    public boolean isBlocked(String playerId, String clientIp) {
        String raw = redisTemplate.opsForValue().get(key(playerId, clientIp));
        if (raw == null) {
            return false;
        }
        try {
            return Long.parseLong(raw) >= maxAttempts;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * Records a failed login attempt and applies window TTL on first increment.
     */
    public void recordFailure(String playerId, String clientIp) {
        Long attempts = redisTemplate.opsForValue().increment(key(playerId, clientIp));
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key(playerId, clientIp), window);
        }
    }

    /**
     * Clears failure counter on successful authentication.
     */
    public void clear(String playerId, String clientIp) {
        redisTemplate.delete(key(playerId, clientIp));
    }

    private String key(String playerId, String clientIp) {
        String ip = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
        return RATE_KEY_PREFIX + playerId + ":" + ip;
    }
}
