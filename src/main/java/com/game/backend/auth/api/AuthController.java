package com.game.backend.auth.api;

import com.game.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication API endpoints for login and logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a player and creates a Redis-backed session token.
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login and create auth session",
        responses = {
            @ApiResponse(responseCode = "200", description = "Login success"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
        }
    )
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest.getRemoteAddr());
    }

    /**
     * Revokes an authenticated player session token.
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Logout and revoke auth session",
        responses = {
            @ApiResponse(responseCode = "200", description = "Logout success"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
        }
    )
    public LogoutResponse logout(@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        authService.logout(authorizationHeader);
        return new LogoutResponse("LOGGED_OUT");
    }
}
