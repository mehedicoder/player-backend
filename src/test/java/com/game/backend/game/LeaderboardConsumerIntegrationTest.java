package com.game.backend.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.backend.game.domain.LeaderboardScore;
import com.game.backend.game.repository.LeaderboardProcessedEventRepository;
import com.game.backend.game.repository.LeaderboardScoreRepository;
import com.game.backend.game.service.PlayerActivityEventEnvelope;
import com.game.backend.game.service.PlayerActivityEventType;
import com.game.backend.game.service.PlayerActivityTopics;
import com.game.backend.player.api.CreatePlayerRequest;
import com.game.backend.player.service.PlayerProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class LeaderboardConsumerIntegrationTest {
    private static final String CONSUMER_GROUP = "leaderboard-test-consumer";

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
        registry.add("spring.kafka.bootstrap-servers", () -> System.getProperty("spring.embedded.kafka.brokers"));
        registry.add("leaderboard.consumer.group-id", () -> CONSUMER_GROUP);
    }

    @Autowired
    private PlayerProfileService profileService;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LeaderboardScoreRepository leaderboardScoreRepository;
    @Autowired
    private LeaderboardProcessedEventRepository processedEventRepository;
    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @BeforeEach
    void waitForLeaderboardConsumerAssignment() {
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            String groupId = container.getContainerProperties().getGroupId();
            if (CONSUMER_GROUP.equals(groupId)) {
                ContainerTestUtils.waitForAssignment(container, 1);
                return;
            }
        }
        throw new AssertionError("Leaderboard consumer container not found for group: " + CONSUMER_GROUP);
    }

    @Test
    void supportedEvent_updatesScore() {
        String playerId = createPlayer();
        PlayerActivityEventEnvelope event = PlayerActivityEventEnvelope.v1(
            "lb-event-" + UUID.randomUUID(),
            PlayerActivityEventType.WALLET_CREDITED_V1,
            playerId,
            null,
            null,
            Map.of("amount", 25L)
        );

        sendEvent(playerId, event);

        LeaderboardScore score = waitForScore(playerId, 25L);
        assertThat(score.getScore()).isEqualTo(25L);
    }

    @Test
    void duplicateEvent_isAppliedOnce() {
        String playerId = createPlayer();
        String eventId = "lb-event-" + UUID.randomUUID();
        PlayerActivityEventEnvelope event = PlayerActivityEventEnvelope.v1(
            eventId,
            PlayerActivityEventType.WALLET_CREDITED_V1,
            playerId,
            null,
            null,
            Map.of("amount", 10L)
        );

        sendEvent(playerId, event);
        sendEvent(playerId, event);

        LeaderboardScore score = waitForScore(playerId, 10L);
        assertThat(score.getScore()).isEqualTo(10L);
        assertThat(processedEventRepository.countByConsumerGroupAndEventId(CONSUMER_GROUP, eventId)).isEqualTo(1L);
    }

    @Test
    void unsupportedEvent_isSkippedAndValidEventStillProcesses() {
        String playerId = createPlayer();
        PlayerActivityEventEnvelope unsupported = PlayerActivityEventEnvelope.v1(
            "lb-event-" + UUID.randomUUID(),
            PlayerActivityEventType.INVENTORY_MUTATED_V1,
            playerId,
            null,
            null,
            Map.of("itemId", "WOOD", "quantityDelta", 1L)
        );
        PlayerActivityEventEnvelope valid = PlayerActivityEventEnvelope.v1(
            "lb-event-" + UUID.randomUUID(),
            PlayerActivityEventType.REWARD_CLAIMED_V1,
            playerId,
            null,
            null,
            Map.of("rewardAmount", 12L)
        );

        sendEvent(playerId, unsupported);
        sendEvent(playerId, valid);

        LeaderboardScore score = waitForScore(playerId, 12L);
        assertThat(score.getScore()).isEqualTo(12L);
    }

    private String createPlayer() {
        String playerId = "leaderboard-" + UUID.randomUUID();
        profileService.createPlayer(new CreatePlayerRequest(playerId, "Leaderboard", playerId + "@example.com", "US"));
        return playerId;
    }

    private LeaderboardScore waitForScore(String playerId, long expectedScore) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(deadline)) {
            var row = leaderboardScoreRepository.findById(playerId);
            if (row.isPresent() && row.get().getScore() == expectedScore) {
                return row.get();
            }
            sleep(100L);
        }
        throw new AssertionError("Timed out waiting for leaderboard score " + expectedScore + " for " + playerId);
    }

    private void sendEvent(String playerId, PlayerActivityEventEnvelope envelope) {
        try {
            kafkaTemplate.send(
                PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1,
                playerId,
                objectMapper.writeValueAsString(envelope)
            ).get(5, TimeUnit.SECONDS);
        } catch (JsonProcessingException serializationError) {
            throw new AssertionError("Failed to serialize event for test", serializationError);
        } catch (Exception publishError) {
            throw new AssertionError("Failed to publish event for test", publishError);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting", interruptedException);
        }
    }
}
