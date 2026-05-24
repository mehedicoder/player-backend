package com.game.backend.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.backend.auth.service.AuthRateLimitedException;
import com.game.backend.auth.service.AuthTokenExpiredException;
import com.game.backend.auth.service.AuthTokenInvalidException;
import com.game.backend.auth.service.AuthTokenMissingException;
import com.game.backend.auth.service.AuthService;
import com.game.backend.auth.service.InvalidCredentialsException;
import com.game.backend.auth.service.SessionData;
import com.game.backend.auth.security.AuthFilter;
import com.game.backend.common.api.GlobalExceptionHandler;
import com.game.backend.config.SecurityConfig;
import com.game.backend.player.api.PlayerProfileController;
import com.game.backend.player.api.PlayerProfileResponse;
import com.game.backend.player.service.PlayerProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, PlayerProfileController.class})
@Import({SecurityConfig.class, AuthFilter.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
class AuthApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private PlayerProfileService playerProfileService;

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class), any()))
            .thenThrow(new InvalidCredentialsException("Invalid credentials"));

        LoginRequest request = new LoginRequest("p1", "bad@example.com");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_rateLimited_returns429() throws Exception {
        when(authService.login(any(LoginRequest.class), any()))
            .thenThrow(new AuthRateLimitedException("Too many failed login attempts"));

        LoginRequest request = new LoginRequest("p1", "bad@example.com");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("AUTH_RATE_LIMITED"));
    }

    @Test
    void protectedEndpoint_missingToken_returns401() throws Exception {
        when(authService.validateToken(null))
            .thenThrow(new AuthTokenMissingException("Authorization header is required"));

        mockMvc.perform(get("/api/v1/players/p1"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_TOKEN_MISSING"));
    }

    @Test
    void protectedEndpoint_invalidToken_returns401() throws Exception {
        when(authService.validateToken("Bearer invalid"))
            .thenThrow(new AuthTokenInvalidException("Authorization header must use Bearer token"));

        mockMvc.perform(get("/api/v1/players/p1")
                .header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
    }

    @Test
    void protectedEndpoint_validToken_returns200() throws Exception {
        SessionData sessionData = new SessionData(
            "token-ok",
            "p1",
            OffsetDateTime.now().minusMinutes(1),
            OffsetDateTime.now().plusMinutes(20)
        );
        when(authService.validateToken("Bearer token-ok")).thenReturn(sessionData);
        when(playerProfileService.getProfile(eq("p1")))
            .thenReturn(new PlayerProfileResponse("p1", "Neo", "neo@example.com", "US", 1, 0L, OffsetDateTime.now(), OffsetDateTime.now()));

        mockMvc.perform(get("/api/v1/players/p1")
                .header("Authorization", "Bearer token-ok"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.playerId").value("p1"));
    }

    @Test
    void logout_invalidToken_returns401() throws Exception {
        doThrow(new AuthTokenInvalidException("Token invalid"))
            .when(authService).logout("Bearer invalid");

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
    }

    @Test
    void logout_expiredToken_returns401() throws Exception {
        doThrow(new AuthTokenExpiredException("Token expired"))
            .when(authService).logout("Bearer expired");

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer expired"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_TOKEN_EXPIRED"));
    }

    @Test
    void logout_validToken_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer token-ok"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("LOGGED_OUT"));
    }
}
