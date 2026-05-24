package com.game.backend.auth.service;

import com.game.backend.auth.api.LoginRequest;
import com.game.backend.auth.api.LoginResponse;
import com.game.backend.player.domain.PlayerProfile;
import com.game.backend.player.repository.PlayerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PlayerProfileRepository playerRepository;
    @Mock
    private AuthSessionStore sessionStore;
    @Mock
    private AuthRateLimiter rateLimiter;
    @Mock
    private TokenGenerator tokenGenerator;
    @Mock
    private AuthHeaderParser headerParser;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_success_returnsTokenAndStoresSession() {
        LoginRequest request = new LoginRequest("p1", "p1@example.com");
        PlayerProfile profile = profile("p1", "p1@example.com");

        when(rateLimiter.isBlocked("p1", "127.0.0.1")).thenReturn(false);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(profile));
        when(tokenGenerator.generate()).thenReturn("token-1");
        when(sessionStore.sessionTtl()).thenReturn(Duration.ofMinutes(30));

        LoginResponse response = authService.login(request, "127.0.0.1");

        assertThat(response.token()).isEqualTo("token-1");
        assertThat(response.expiresAt()).isAfter(OffsetDateTime.now().plusMinutes(29));
        verify(sessionStore).save(any(SessionData.class));
        verify(rateLimiter).clear("p1", "127.0.0.1");
    }

    @Test
    void login_invalidCredentials_throwsAndRecordsFailure() {
        LoginRequest request = new LoginRequest("p2", "bad@example.com");
        PlayerProfile profile = profile("p2", "good@example.com");

        when(rateLimiter.isBlocked("p2", "127.0.0.1")).thenReturn(false);
        when(playerRepository.findById("p2")).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
            .isInstanceOf(InvalidCredentialsException.class);

        verify(rateLimiter).recordFailure("p2", "127.0.0.1");
    }

    @Test
    void login_rateLimited_throws() {
        LoginRequest request = new LoginRequest("p3", "p3@example.com");
        when(rateLimiter.isBlocked("p3", "127.0.0.1")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
            .isInstanceOf(AuthRateLimitedException.class);
    }

    @Test
    void logout_deletesSession() {
        when(headerParser.extractBearerToken("Bearer abc")).thenReturn("abc");
        SessionData active = new SessionData("abc", "p1", OffsetDateTime.now().minusMinutes(2), OffsetDateTime.now().plusMinutes(10));
        when(sessionStore.get("abc")).thenReturn(Optional.of(active));
        authService.logout("Bearer abc");
        verify(sessionStore).delete("abc");
    }

    @Test
    void logout_invalidToken_throws() {
        when(headerParser.extractBearerToken("Bearer missing")).thenReturn("missing");
        when(sessionStore.get("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout("Bearer missing"))
            .isInstanceOf(AuthTokenInvalidException.class);
    }

    @Test
    void validateToken_expired_throwsAndDeletes() {
        when(headerParser.extractBearerToken("Bearer abc")).thenReturn("abc");
        SessionData expired = new SessionData("abc", "p1", OffsetDateTime.now().minusHours(2), OffsetDateTime.now().minusMinutes(1));
        when(sessionStore.get("abc")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.validateToken("Bearer abc"))
            .isInstanceOf(AuthTokenExpiredException.class);
        verify(sessionStore).delete("abc");
    }

    private PlayerProfile profile(String playerId, String email) {
        PlayerProfile profile = new PlayerProfile();
        profile.setPlayerId(playerId);
        profile.setEmail(email);
        profile.setDisplayName("Player");
        profile.setCountryCode("US");
        profile.setLevel(1);
        profile.setExperiencePoints(0L);
        return profile;
    }
}
