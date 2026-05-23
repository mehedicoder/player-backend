package com.game.backend.player.api;

import com.game.backend.player.service.PlayerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/players")
@Tag(name = "Player Profile", description = "Player profile create/read/update operations")
public class PlayerProfileController {

    private final PlayerProfileService service;

    public PlayerProfileController(PlayerProfileService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
        summary = "Create or seed player profile",
        responses = {
            @ApiResponse(responseCode = "200", description = "Profile created",
                content = @Content(schema = @Schema(implementation = PlayerProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request")
        }
    )
    public PlayerProfileResponse create(@Valid @RequestBody CreatePlayerRequest request) {
        return service.createPlayer(request);
    }

    @GetMapping("/{playerId}")
    @Operation(
        summary = "Get player profile by ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Profile returned",
                content = @Content(schema = @Schema(implementation = PlayerProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "Player not found")
        }
    )
    public PlayerProfileResponse get(@PathVariable @NotBlank String playerId) {
        return service.getProfile(playerId);
    }

    @PutMapping("/{playerId}")
    @Operation(
        summary = "Update player profile",
        responses = {
            @ApiResponse(responseCode = "200", description = "Profile updated",
                content = @Content(schema = @Schema(implementation = PlayerProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Player not found")
        }
    )
    public PlayerProfileResponse update(
        @PathVariable @NotBlank String playerId,
        @Valid @RequestBody UpdatePlayerProfileRequest request
    ) {
        return service.updateProfile(playerId, request);
    }
}
