package com.game.backend.player.service;

import com.game.backend.player.api.CreatePlayerRequest;
import com.game.backend.player.api.PlayerProfileResponse;
import com.game.backend.player.api.UpdatePlayerProfileRequest;
import com.game.backend.player.domain.PlayerProfile;
import com.game.backend.player.repository.PlayerProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PlayerProfileService {

    private static final Logger log = LoggerFactory.getLogger(PlayerProfileService.class);

    private final PlayerProfileRepository repository;
    private final PlayerProfileCache cache;

    public PlayerProfileService(PlayerProfileRepository repository, PlayerProfileCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    @Transactional
    public PlayerProfileResponse createPlayer(CreatePlayerRequest request) {
        String playerId = request.playerId() == null || request.playerId().isBlank()
            ? UUID.randomUUID().toString()
            : request.playerId();

        if (repository.existsById(playerId)) {
            throw new IllegalArgumentException("Player already exists for playerId=" + playerId);
        }

        PlayerProfile profile = new PlayerProfile();
        profile.setPlayerId(playerId);
        profile.setDisplayName(request.displayName());
        profile.setEmail(request.email());
        profile.setCountryCode(request.countryCode());
        profile.setLevel(1);
        profile.setExperiencePoints(0L);

        PlayerProfile saved = repository.save(profile);
        cache.put(saved);
        return PlayerProfileResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PlayerProfileResponse getProfile(String playerId) {
        try {
            Optional<PlayerProfile> cached = cache.get(playerId);
            if (cached.isPresent()) {
                return PlayerProfileResponse.from(cached.get());
            }
        } catch (Exception e) {
            log.warn("Redis read failed for playerId={}, falling back to DB", playerId, e);
        }

        PlayerProfile profile = repository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));

        cache.put(profile);
        return PlayerProfileResponse.from(profile);
    }

    @Transactional
    public PlayerProfileResponse updateProfile(String playerId, UpdatePlayerProfileRequest request) {
        PlayerProfile profile = repository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));

        profile.setDisplayName(request.displayName());
        profile.setEmail(request.email());
        profile.setCountryCode(request.countryCode());

        PlayerProfile saved = repository.save(profile);
        cache.put(saved);
        return PlayerProfileResponse.from(saved);
    }
}
