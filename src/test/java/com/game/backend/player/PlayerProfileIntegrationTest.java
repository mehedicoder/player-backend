package com.game.backend.player;

import com.game.backend.player.api.CreatePlayerRequest;
import com.game.backend.player.api.PlayerProfileResponse;
import com.game.backend.player.api.UpdatePlayerProfileRequest;
import com.game.backend.player.service.PlayerProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PlayerProfileIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("playerdb")
        .withUsername("player")
        .withPassword("player");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("player.profile.cache.ttl", () -> "PT1M");
    }

    @Autowired
    private PlayerProfileService service;

    @Test
    void createReadUpdateProfile_withMySqlAndRedis() {
        PlayerProfileResponse created = service.createPlayer(
            new CreatePlayerRequest("it-player-1", "Initial", "it-player-1@example.com", "US")
        );
        assertThat(created.playerId()).isEqualTo("it-player-1");

        PlayerProfileResponse read = service.getProfile("it-player-1");
        assertThat(read.displayName()).isEqualTo("Initial");

        PlayerProfileResponse updated = service.updateProfile(
            "it-player-1",
            new UpdatePlayerProfileRequest("Updated", "it-player-1-updated@example.com", "DE")
        );
        assertThat(updated.displayName()).isEqualTo("Updated");
        assertThat(updated.countryCode()).isEqualTo("DE");
    }
}
