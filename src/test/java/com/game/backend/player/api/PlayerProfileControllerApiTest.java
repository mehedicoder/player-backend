package com.game.backend.player.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.backend.auth.service.AuthService;
import com.game.backend.player.service.PlayerNotFoundException;
import com.game.backend.player.service.PlayerProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlayerProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlayerProfileControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlayerProfileService service;

    @MockBean
    private AuthService authService;

    @Test
    void create_returns200() throws Exception {
        PlayerProfileResponse response = response("p1", "Neo", "neo@example.com", "US");
        when(service.createPlayer(any(CreatePlayerRequest.class))).thenReturn(response);

        CreatePlayerRequest request = new CreatePlayerRequest("p1", "Neo", "neo@example.com", "US");
        mockMvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.playerId").value("p1"));
    }

    @Test
    void create_invalidPayload_returns400() throws Exception {
        CreatePlayerRequest request = new CreatePlayerRequest("p2", "", "bad-email", "u");
        mockMvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(service.getProfile("missing")).thenThrow(new PlayerNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/players/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PLAYER_NOT_FOUND"));
    }

    @Test
    void update_returns200() throws Exception {
        PlayerProfileResponse response = response("p3", "Updated", "updated@example.com", "DE");
        when(service.updateProfile(eq("p3"), any(UpdatePlayerProfileRequest.class))).thenReturn(response);

        UpdatePlayerProfileRequest request = new UpdatePlayerProfileRequest("Updated", "updated@example.com", "DE");
        mockMvc.perform(put("/api/v1/players/p3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.countryCode").value("DE"));
    }

    @Test
    void create_duplicateEmail_returns409() throws Exception {
        when(service.createPlayer(any(CreatePlayerRequest.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate email"));

        CreatePlayerRequest request = new CreatePlayerRequest("p4", "Neo", "dup@example.com", "US");
        mockMvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
    }

    private PlayerProfileResponse response(String playerId, String name, String email, String country) {
        OffsetDateTime now = OffsetDateTime.now();
        return new PlayerProfileResponse(playerId, name, email, country, 1, 0L, now, now);
    }
}
