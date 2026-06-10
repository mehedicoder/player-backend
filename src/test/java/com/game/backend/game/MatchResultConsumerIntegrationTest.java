package com.game.backend.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.backend.game.domain.MatchResultProjection;
import com.game.backend.game.repository.MatchResultProcessedEventRepository;
import com.game.backend.game.repository.MatchResultProjectionRepository;
import com.game.backend.game.repository.NotificationJobRepository;
import com.game.backend.game.service.MatchResultEventEnvelope;
import com.game.backend.game.service.MatchResultEventType;
import com.game.backend.game.service.MatchResultIngestionService;
import com.game.backend.game.service.MatchResultTopics;
import com.game.backend.player.api.CreatePlayerRequest;
import com.game.backend.player.service.PlayerProfileService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {MatchResultTopics.MATCH_RESULTS_V1, MatchResultTopics.MATCH_RESULTS_V1_DLQ})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class MatchResultConsumerIntegrationTest {

    private static final String CONSUMER_GROUP = "match-result-test-consumer";

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
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.producer.properties.spring.json.add.type.headers", () -> false);
        registry.add("match-result.consumer.group-id", () -> CONSUMER_GROUP);
        registry.add("notification.worker.enabled", () -> false);
    }

    @Autowired
    private PlayerProfileService profileService;
    private KafkaOperations<String, String> inputKafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MatchResultProjectionRepository projectionRepository;
    @Autowired
    private MatchResultProcessedEventRepository processedEventRepository;
    @Autowired
    private NotificationJobRepository notificationJobRepository;
    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    @SpyBean
    private MatchResultIngestionService ingestionService;

    @BeforeEach
    void waitForMatchResultConsumerAssignment() {
        if (inputKafkaTemplate == null) {
            inputKafkaTemplate = inputKafkaTemplate();
        }
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            String groupId = container.getContainerProperties().getGroupId();
            if (CONSUMER_GROUP.equals(groupId)) {
                ContainerTestUtils.waitForAssignment(container, 1);
                return;
            }
        }
        throw new AssertionError("Match-result consumer container not found for group: " + CONSUMER_GROUP);
    }

    @Test
    void supportedEvent_createsProjectionDedupeAndNotificationJob() {
        String playerId = createPlayer();
        String eventId = "match-event-" + UUID.randomUUID();
        MatchResultEventEnvelope event = event(eventId, playerId, "match-1", "WIN", 35);

        sendEvent(playerId, event);

        MatchResultProjection projection = waitForProjection(playerId, "match-1");
        assertThat(projection.getResult().name()).isEqualTo("WIN");
        assertThat(projection.getScoreDelta()).isEqualTo(35);
        assertThat(processedEventRepository.countByConsumerGroupAndEventId(CONSUMER_GROUP, eventId)).isEqualTo(1L);
        assertThat(notificationJobRepository.countByEventId(eventId)).isEqualTo(1L);
    }

    @Test
    void duplicateEvent_isAppliedOnceAndCreatesOneNotificationJob() {
        String playerId = createPlayer();
        String eventId = "match-event-" + UUID.randomUUID();
        MatchResultEventEnvelope event = event(eventId, playerId, "match-dup", "DRAW", 0);

        sendEvent(playerId, event);
        sendEvent(playerId, event);

        waitForProjection(playerId, "match-dup");
        waitUntil(() -> processedEventRepository.countByConsumerGroupAndEventId(CONSUMER_GROUP, eventId) == 1L);
        assertThat(processedEventRepository.countByConsumerGroupAndEventId(CONSUMER_GROUP, eventId)).isEqualTo(1L);
        assertThat(notificationJobRepository.countByEventId(eventId)).isEqualTo(1L);
    }

    @Test
    void invalidEvent_isSkippedAndValidEventStillProcesses() {
        String playerId = createPlayer();
        sendRaw(playerId, "{\"eventId\":\"bad\",\"eventType\":\"unsupported.v1\",\"playerId\":\"" + playerId + "\",\"occurredAt\":\"2026-06-08T12:00:00Z\",\"payload\":{}}");
        MatchResultEventEnvelope valid = event("match-event-" + UUID.randomUUID(), playerId, "match-valid", "LOSS", -10);

        sendEvent(playerId, valid);

        MatchResultProjection projection = waitForProjection(playerId, "match-valid");
        assertThat(projection.getResult().name()).isEqualTo("LOSS");
        assertThat(projection.getScoreDelta()).isEqualTo(-10);
    }

    @Test
    void missingPlayer_isSkippedAsInvalidAndDoesNotRouteToDlq() throws Exception {
        String missingPlayerId = "missing-" + UUID.randomUUID();
        String eventId = "match-event-" + UUID.randomUUID();
        MatchResultEventEnvelope event = event(eventId, missingPlayerId, "match-dlq", "WIN", 1);

        try (Consumer<String, String> consumer = dlqConsumer()) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, MatchResultTopics.MATCH_RESULTS_V1_DLQ);
            sendEvent(missingPlayerId, event);
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(
                consumer,
                Duration.ofSeconds(2)
            );
            assertThat(records.isEmpty()).isTrue();
        }
    }

    @Test
    void retryableTransientFailure_routesToDlqAfterRetries() throws Exception {
        String playerId = createPlayer();
        String eventId = "match-event-" + UUID.randomUUID();
        MatchResultEventEnvelope event = event(eventId, playerId, "match-retry", "WIN", 1);
        doThrow(new TransientDataAccessResourceException("db_down"))
            .when(ingestionService)
            .processEvent(argThat(envelope -> eventId.equals(envelope.eventId())), eq(CONSUMER_GROUP));

        try (Consumer<String, String> consumer = dlqConsumer()) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, MatchResultTopics.MATCH_RESULTS_V1_DLQ);
            sendEvent(playerId, event);
            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
                consumer,
                MatchResultTopics.MATCH_RESULTS_V1_DLQ,
                Duration.ofSeconds(20)
            );
            JsonNode payload = objectMapper.readTree(record.value());
            assertThat(payload.path("originalTopic").asText()).isEqualTo(MatchResultTopics.MATCH_RESULTS_V1);
            assertThat(payload.path("eventId").asText()).isEqualTo(eventId);
            assertThat(payload.path("eventType").asText()).isEqualTo(MatchResultEventType.MATCH_RESULT_RECORDED_V1);
            assertThat(payload.path("consumerGroup").asText()).isEqualTo(CONSUMER_GROUP);
            assertThat(payload.path("errorCategory").asText()).isEqualTo("TRANSIENT_RETRY_EXHAUSTED");
            assertThat(payload.hasNonNull("failedAt")).isTrue();
        }
        verify(ingestionService, timeout(20_000).times(3))
            .processEvent(argThat(envelope -> eventId.equals(envelope.eventId())), eq(CONSUMER_GROUP));
    }

    private Consumer<String, String> dlqConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
            "match-result-dlq-test-" + UUID.randomUUID(),
            "true",
            embeddedKafkaBroker
        );
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<String, String>(props).createConsumer();
    }

    private String createPlayer() {
        String playerId = "match-" + UUID.randomUUID();
        profileService.createPlayer(new CreatePlayerRequest(playerId, "Match", playerId + "@example.com", "US"));
        return playerId;
    }

    private MatchResultProjection waitForProjection(String playerId, String matchId) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(deadline)) {
            var row = projectionRepository.findByPlayerIdAndMatchId(playerId, matchId);
            if (row.isPresent()) {
                return row.get();
            }
            sleep(100L);
        }
        throw new AssertionError("Timed out waiting for match projection for " + playerId + " / " + matchId);
    }

    private void waitUntil(BooleanSupplier condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            sleep(100L);
        }
        throw new AssertionError("Timed out waiting for condition");
    }

    private MatchResultEventEnvelope event(String eventId, String playerId, String matchId, String result, int scoreDelta) {
        return new MatchResultEventEnvelope(
            eventId,
            MatchResultEventType.MATCH_RESULT_RECORDED_V1,
            playerId,
            "2026-06-08T12:00:00Z",
            Map.of("matchId", matchId, "result", result, "scoreDelta", scoreDelta)
        );
    }

    private void sendEvent(String playerId, MatchResultEventEnvelope envelope) {
        try {
            sendRaw(playerId, objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException serializationError) {
            throw new AssertionError("Failed to serialize event for test", serializationError);
        }
    }

    private void sendRaw(String playerId, String payload) {
        try {
            inputKafkaTemplate.send(MatchResultTopics.MATCH_RESULTS_V1, playerId, payload).get(5, TimeUnit.SECONDS);
        } catch (Exception publishError) {
            throw new AssertionError("Failed to publish event for test", publishError);
        }
    }

    private KafkaOperations<String, String> inputKafkaTemplate() {
        Map<String, Object> props = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        props.put("key.serializer", StringSerializer.class);
        props.put("value.serializer", StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting", interruptedException);
        }
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }
}
