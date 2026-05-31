package com.game.backend.game;

import com.game.backend.game.api.InventoryMutationRequest;
import com.game.backend.game.api.InventoryOperation;
import com.game.backend.game.api.RewardClaimResponse;
import com.game.backend.game.api.WalletMutationRequest;
import com.game.backend.game.api.WalletMutationType;
import com.game.backend.game.api.WalletResponse;
import com.game.backend.game.repository.RewardClaimRepository;
import com.game.backend.game.repository.WalletIdempotencyRepository;
import com.game.backend.game.repository.WalletLedgerRepository;
import com.game.backend.game.service.InventoryService;
import com.game.backend.game.service.PlayerActivityEventType;
import com.game.backend.game.service.PlayerActivityPublisher;
import com.game.backend.game.service.PlayerActivityTopics;
import com.game.backend.game.service.RewardService;
import com.game.backend.game.service.WalletService;
import com.game.backend.player.api.CreatePlayerRequest;
import com.game.backend.player.service.PlayerProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class GameStateIntegrationTest {

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
    }

    @Autowired
    private PlayerProfileService profileService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private RewardService rewardService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private WalletLedgerRepository walletLedgerRepository;
    @Autowired
    private WalletIdempotencyRepository walletIdempotencyRepository;
    @Autowired
    private RewardClaimRepository rewardClaimRepository;

    @MockBean
    private PlayerActivityPublisher playerActivityPublisher;

    @Test
    void walletMutation_duplicateIdempotencyKey_isNotAppliedTwice() {
        String playerId = createPlayer();
        WalletMutationRequest first = new WalletMutationRequest(WalletMutationType.CREDIT, 100, "idem-1", "quest");
        WalletResponse r1 = walletService.mutateWallet(playerId, first);
        WalletResponse r2 = walletService.mutateWallet(playerId, first);

        assertThat(r1.balance()).isEqualTo(100L);
        assertThat(r2.balance()).isEqualTo(100L);
        assertThat(walletLedgerRepository.countByPlayerId(playerId)).isEqualTo(1L);
        assertThat(walletIdempotencyRepository.countByPlayerId(playerId)).isEqualTo(1L);
        verify(playerActivityPublisher, times(1))
            .publish(eq(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1), eq(playerId), argThat(event ->
                event.eventType().equals(PlayerActivityEventType.WALLET_CREDITED_V1)
                    && event.playerId().equals(playerId)
                    && event.idempotencyKey().equals("idem-1")
                    && "QUEST".equals(event.payload().get("reason"))
            ));
    }

    @Test
    void rewardClaim_duplicateReward_returnsOriginalAndNoDoubleCredit() {
        String playerId = createPlayer();
        RewardClaimResponse r1 = rewardService.claim(playerId, "reward-1", "reward-idem-1", 50);
        RewardClaimResponse r2 = rewardService.claim(playerId, "reward-1", "reward-idem-2", 50);

        assertThat(r1.balanceAfter()).isEqualTo(r2.balanceAfter());
        assertThat(r2.rewardAmount()).isEqualTo(50L);
        assertThat(rewardClaimRepository.countByPlayerId(playerId)).isEqualTo(1L);
        assertThat(walletLedgerRepository.countByPlayerId(playerId)).isEqualTo(1L);
        verify(playerActivityPublisher, times(1))
            .publish(eq(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1), eq(playerId), argThat(event ->
                event.eventType().equals(PlayerActivityEventType.REWARD_CLAIMED_V1)
                    && event.playerId().equals(playerId)
                    && "reward-1".equals(event.payload().get("rewardId"))
            ));
    }

    @Test
    void walletMutation_concurrentCredits_produceConsistentBalance() throws ExecutionException, InterruptedException {
        String playerId = createPlayer();
        walletService.getWallet(playerId);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<WalletResponse>> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                int idx = i;
                tasks.add(() -> walletService.mutateWallet(
                    playerId,
                    new WalletMutationRequest(WalletMutationType.CREDIT, 10, "concurrent-" + idx, "batch-credit")));
            }
            List<Future<WalletResponse>> futures = executor.invokeAll(tasks);
            for (Future<WalletResponse> future : futures) {
                future.get();
            }

            WalletResponse current = walletService.getWallet(playerId);
            assertThat(current.balance()).isEqualTo(200L);
            assertThat(walletLedgerRepository.countByPlayerId(playerId)).isEqualTo(20L);
            verify(playerActivityPublisher, times(20))
                .publish(eq(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1), eq(playerId), argThat(event ->
                    event.eventType().equals(PlayerActivityEventType.WALLET_CREDITED_V1)
                        && "BATCH_CREDIT".equals(event.payload().get("reason"))
                ));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void inventoryMutation_success_publishesEvent() {
        String playerId = createPlayer();
        inventoryService.mutateInventory(playerId, new InventoryMutationRequest("WOOD", InventoryOperation.ADD, 5));

        verify(playerActivityPublisher, atLeastOnce())
            .publish(eq(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1), eq(playerId), argThat(event ->
                event.eventType().equals(PlayerActivityEventType.INVENTORY_MUTATED_V1)
                    && "WOOD".equals(event.payload().get("itemId"))
                    && String.valueOf(event.payload().get("mutationId")).startsWith("inventory-mutation-event-")
            ));
    }

    @Test
    void inventoryMutation_distinctOperations_produceDistinctMutationIds() {
        String playerId = createPlayer();
        inventoryService.mutateInventory(playerId, new InventoryMutationRequest("WOOD", InventoryOperation.ADD, 5));
        inventoryService.mutateInventory(playerId, new InventoryMutationRequest("WOOD", InventoryOperation.REMOVE, 5));

        var eventCaptor = forClass(com.game.backend.game.service.PlayerActivityEventEnvelope.class);
        verify(playerActivityPublisher, times(2))
            .publish(eq(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1), eq(playerId), eventCaptor.capture());

        var mutationIds = eventCaptor.getAllValues()
            .stream()
            .map(event -> String.valueOf(event.payload().get("mutationId")))
            .collect(Collectors.toList());

        assertThat(mutationIds).hasSize(2);
        assertThat(mutationIds.get(0)).startsWith("inventory-mutation-event-");
        assertThat(mutationIds.get(1)).startsWith("inventory-mutation-event-");
        assertThat(mutationIds.get(0)).isNotEqualTo(mutationIds.get(1));
    }

    @Test
    void walletMutation_parallelSameIdempotencyKey_isIdempotent() throws Exception {
        String playerId = createPlayer();
        walletService.getWallet(playerId);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<WalletResponse>> tasks = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                tasks.add(() -> walletService.mutateWallet(
                    playerId,
                    new WalletMutationRequest(WalletMutationType.CREDIT, 15, "same-key", "parallel-idem")));
            }

            List<Future<WalletResponse>> futures = executor.invokeAll(tasks);
            for (Future<WalletResponse> future : futures) {
                assertThat(future.get().balance()).isEqualTo(15L);
            }

            WalletResponse current = walletService.getWallet(playerId);
            assertThat(current.balance()).isEqualTo(15L);
            assertThat(walletLedgerRepository.countByPlayerId(playerId)).isEqualTo(1L);
            assertThat(walletIdempotencyRepository.countByPlayerId(playerId)).isEqualTo(1L);
            verify(playerActivityPublisher, times(1))
                .publish(eq(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1), eq(playerId), argThat(event ->
                    event.eventType().equals(PlayerActivityEventType.WALLET_CREDITED_V1)
                        && "PARALLEL_IDEM".equals(event.payload().get("reason"))
                ));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rewardClaim_parallelSameReward_isIdempotent() throws Exception {
        String playerId = createPlayer();
        walletService.getWallet(playerId);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<RewardClaimResponse>> tasks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                tasks.add(() -> rewardService.claim(playerId, "reward-par", "reward-idem-same", 30));
            }

            List<Future<RewardClaimResponse>> futures = executor.invokeAll(tasks);
            for (Future<RewardClaimResponse> future : futures) {
                assertThat(future.get().balanceAfter()).isEqualTo(30L);
            }

            assertThat(rewardClaimRepository.countByPlayerId(playerId)).isEqualTo(1L);
            assertThat(walletLedgerRepository.countByPlayerId(playerId)).isEqualTo(1L);
            verify(playerActivityPublisher, times(1))
                .publish(eq(PlayerActivityTopics.PLAYER_ACTIVITY_EVENTS_V1), eq(playerId), argThat(event ->
                    event.eventType().equals(PlayerActivityEventType.REWARD_CLAIMED_V1)
                        && "reward-par".equals(event.payload().get("rewardId"))
                ));
        } finally {
            executor.shutdownNow();
        }
    }

    private String createPlayer() {
        String playerId = "phase4-" + UUID.randomUUID();
        profileService.createPlayer(new CreatePlayerRequest(playerId, "Phase4", playerId + "@example.com", "US"));
        return playerId;
    }
}
