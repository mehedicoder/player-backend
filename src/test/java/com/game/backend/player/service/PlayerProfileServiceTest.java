package com.game.backend.player.service;

import com.game.backend.player.api.CreatePlayerRequest;
import com.game.backend.player.api.PlayerProfileResponse;
import com.game.backend.player.api.UpdatePlayerProfileRequest;
import com.game.backend.player.domain.PlayerProfile;
import com.game.backend.player.repository.PlayerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerProfileServiceTest {

    @Mock
    private PlayerProfileRepository repository;

    @Mock
    private PlayerProfileCache cache;

    @InjectMocks
    private PlayerProfileService service;

    @Test
    void getProfile_returnsCacheHit() {
        PlayerProfile profile = profile("player-1");
        when(cache.get("player-1")).thenReturn(Optional.of(profile));

        PlayerProfileResponse response = service.getProfile("player-1");

        assertThat(response.playerId()).isEqualTo("player-1");
        verify(repository, never()).findById(any());
    }

    @Test
    void getProfile_returnsDbOnCacheMiss_andStoresCache() {
        PlayerProfile profile = profile("player-2");
        when(cache.get("player-2")).thenReturn(Optional.empty());
        when(repository.findById("player-2")).thenReturn(Optional.of(profile));

        PlayerProfileResponse response = service.getProfile("player-2");

        assertThat(response.playerId()).isEqualTo("player-2");
        verify(cache).put(profile);
    }

    @Test
    void getProfile_fallsBackToDbWhenCacheThrows() {
        PlayerProfile profile = profile("player-3");
        when(cache.get("player-3")).thenThrow(new RuntimeException("redis down"));
        when(repository.findById("player-3")).thenReturn(Optional.of(profile));

        PlayerProfileResponse response = service.getProfile("player-3");

        assertThat(response.playerId()).isEqualTo("player-3");
        verify(cache).put(profile);
    }

    @Test
    void getProfile_throwsNotFound() {
        when(cache.get("missing")).thenReturn(Optional.empty());
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile("missing"))
            .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void updateProfile_updatesAndCaches() {
        PlayerProfile profile = profile("player-4");
        UpdatePlayerProfileRequest request = new UpdatePlayerProfileRequest("Neo", "neo@example.com", "DE");

        when(repository.findById("player-4")).thenReturn(Optional.of(profile));
        when(repository.save(any(PlayerProfile.class))).thenAnswer(i -> i.getArgument(0));

        PlayerProfileResponse response = service.updateProfile("player-4", request);

        assertThat(response.displayName()).isEqualTo("Neo");
        assertThat(response.email()).isEqualTo("neo@example.com");
        verify(cache).put(any(PlayerProfile.class));
    }

    @Test
    void createPlayer_createsAndCaches() {
        CreatePlayerRequest request = new CreatePlayerRequest("player-5", "Max", "max@example.com", "US");
        when(repository.existsById("player-5")).thenReturn(false);
        when(repository.save(any(PlayerProfile.class))).thenAnswer(i -> {
            PlayerProfile p = i.getArgument(0);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        PlayerProfileResponse response = service.createPlayer(request);

        assertThat(response.playerId()).isEqualTo("player-5");
        verify(cache).put(any(PlayerProfile.class));
    }

    private PlayerProfile profile(String playerId) {
        PlayerProfile p = new PlayerProfile();
        p.setPlayerId(playerId);
        p.setDisplayName("Player");
        p.setEmail(playerId + "@example.com");
        p.setCountryCode("US");
        p.setLevel(1);
        p.setExperiencePoints(100L);
        p.setCreatedAt(OffsetDateTime.now());
        p.setUpdatedAt(OffsetDateTime.now());
        return p;
    }
}
