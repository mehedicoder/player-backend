package com.game.backend.game.api;

import com.game.backend.game.service.InventoryService;
import com.game.backend.game.service.RewardService;
import com.game.backend.game.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST APIs for inventory, wallet, and reward claims.
 */
@RestController
@Validated
@RequestMapping("/api/v1/players/{playerId}")
public class GameStateController {

    private final InventoryService inventoryService;
    private final WalletService walletService;
    private final RewardService rewardService;

    public GameStateController(
        InventoryService inventoryService,
        WalletService walletService,
        RewardService rewardService
    ) {
        this.inventoryService = inventoryService;
        this.walletService = walletService;
        this.rewardService = rewardService;
    }

    /**
     * Returns inventory snapshot.
     */
    @GetMapping("/inventory")
    public InventoryResponse getInventory(@PathVariable @NotBlank String playerId) {
        return inventoryService.getInventory(playerId);
    }

    /**
     * Applies inventory mutation.
     */
    @PostMapping("/inventory/mutations")
    public InventoryResponse mutateInventory(
        @PathVariable @NotBlank String playerId,
        @Valid @RequestBody InventoryMutationRequest request
    ) {
        return inventoryService.mutateInventory(playerId, request);
    }

    /**
     * Returns wallet state.
     */
    @GetMapping("/wallet")
    public WalletResponse getWallet(@PathVariable @NotBlank String playerId) {
        return walletService.getWallet(playerId);
    }

    /**
     * Applies idempotent wallet mutation.
     */
    @PostMapping("/wallet/mutations")
    public WalletResponse mutateWallet(
        @PathVariable @NotBlank String playerId,
        @Valid @RequestBody WalletMutationRequest request
    ) {
        return walletService.mutateWallet(playerId, request);
    }

    /**
     * Claims reward idempotently.
     */
    @PostMapping("/rewards/{rewardId}/claim")
    public RewardClaimResponse claimReward(
        @PathVariable @NotBlank String playerId,
        @PathVariable @NotBlank String rewardId,
        @Valid @RequestBody RewardClaimRequest request
    ) {
        return rewardService.claim(playerId, rewardId, request.idempotencyKey(), request.rewardAmount());
    }
}

