package com.game.backend.player.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.backend.player.domain.PlayerProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis cache adapter for player profile payloads.
 */
@Component
public class PlayerProfileCache {

    private static final Logger log = LoggerFactory.getLogger(PlayerProfileCache.class);
    private static final String CACHE_KEY_PREFIX = "player:profile:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public PlayerProfileCache(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        @Value("${player.profile.cache.ttl:PT5M}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    /**
     * Reads a cached player profile by player identifier.
     */
    public Optional<PlayerProfile> get(String playerId) {
        String payload = redisTemplate.opsForValue().get(key(playerId));
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, PlayerProfile.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize player profile cache for playerId={}", playerId, e);
            return Optional.empty();
        }
    }

    /**
     * Writes a player profile cache entry with configured TTL.
     */
    public void put(PlayerProfile profile) {
        try {
            String payload = objectMapper.writeValueAsString(profile);
            redisTemplate.opsForValue().set(key(profile.getPlayerId()), payload, ttl);
        } catch (Exception e) {
            log.warn("Failed to write player profile cache for playerId={}", profile.getPlayerId(), e);
        }
    }

    private String key(String playerId) {
        return CACHE_KEY_PREFIX + playerId;
    }
}
