package com.game.backend.auth.service;

import com.game.backend.auth.api.LoginRequest;
import com.game.backend.auth.api.LoginResponse;
import com.game.backend.player.domain.PlayerProfile;
import com.game.backend.player.repository.PlayerProfileRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Authentication service for login, logout, and token validation.
 */
@Service
public class AuthService {

    private final PlayerProfileRepository playerRepository;
    private final AuthSessionStore sessionStore;
    private final AuthRateLimiter rateLimiter;
    private final TokenGenerator tokenGenerator;
    private final AuthHeaderParser headerParser;

    public AuthService(
        PlayerProfileRepository playerRepository,
        AuthSessionStore sessionStore,
        AuthRateLimiter rateLimiter,
        TokenGenerator tokenGenerator,
        AuthHeaderParser headerParser
    ) {
        this.playerRepository = playerRepository;
        this.sessionStore = sessionStore;
        this.rateLimiter = rateLimiter;
        this.tokenGenerator = tokenGenerator;
        this.headerParser = headerParser;
    }

    /**
     * Authenticates player credentials, applies rate limiting, and creates a Redis-backed session.
     */
    public LoginResponse login(LoginRequest request, String clientIp) {
        if (rateLimiter.isBlocked(request.playerId(), clientIp)) {
            throw new AuthRateLimitedException("Too many failed login attempts");
        }

        PlayerProfile player = playerRepository.findById(request.playerId())
            .orElseThrow(() -> invalidCredentials(request.playerId(), clientIp));

        if (!player.getEmail().equalsIgnoreCase(request.email())) {
            throw invalidCredentials(request.playerId(), clientIp);
        }

        rateLimiter.clear(request.playerId(), clientIp);

        String token = tokenGenerator.generate();
        OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiresAt = issuedAt.plus(sessionStore.sessionTtl());

        sessionStore.save(new SessionData(token, player.getPlayerId(), issuedAt, expiresAt));
        return new LoginResponse(token, expiresAt);
    }

    /**
     * Logs out by revoking the provided bearer token.
     */
    public void logout(String authorizationHeader) {
        SessionData sessionData = validateToken(authorizationHeader);
        sessionStore.delete(sessionData.token());
    }

    /**
     * Validates bearer token and returns active session data.
     */
    public SessionData validateToken(String authorizationHeader) {
        String token = headerParser.extractBearerToken(authorizationHeader);
        SessionData sessionData = sessionStore.get(token).orElseThrow(() -> new AuthTokenInvalidException("Token invalid"));
        if (sessionData.expiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            sessionStore.delete(token);
            throw new AuthTokenExpiredException("Token expired");
        }
        return sessionData;
    }

    private RuntimeException invalidCredentials(String playerId, String clientIp) {
        rateLimiter.recordFailure(playerId, clientIp);
        return new InvalidCredentialsException("Invalid credentials");
    }
}
