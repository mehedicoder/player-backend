package com.game.backend.game.service;

import com.game.backend.game.domain.LeaderboardProcessedEvent;
import com.game.backend.game.domain.LeaderboardScore;
import com.game.backend.game.repository.LeaderboardProcessedEventRepository;
import com.game.backend.game.repository.LeaderboardScoreRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Applies idempotent leaderboard score updates from activity events.
 */
@Service
public class LeaderboardScoreUpdateService {

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
        PlayerActivityEventType.WALLET_CREDITED_V1,
        PlayerActivityEventType.WALLET_DEBITED_V1,
        PlayerActivityEventType.REWARD_CLAIMED_V1
    );

    private final LeaderboardScoreRepository leaderboardScoreRepository;
    private final LeaderboardProcessedEventRepository processedEventRepository;
    private final LeaderboardConsumerProperties properties;

    public LeaderboardScoreUpdateService(
        LeaderboardScoreRepository leaderboardScoreRepository,
        LeaderboardProcessedEventRepository processedEventRepository,
        LeaderboardConsumerProperties properties
    ) {
        this.leaderboardScoreRepository = leaderboardScoreRepository;
        this.processedEventRepository = processedEventRepository;
        this.properties = properties;
    }

    /**
     * Processes one envelope in a single transaction and returns processing outcome.
     */
    @Transactional
    public LeaderboardProcessingOutcome processEvent(PlayerActivityEventEnvelope envelope, String consumerGroup) {
        validateEnvelope(envelope);
        String eventId = envelope.eventId();
        if (processedEventRepository.existsByConsumerGroupAndEventId(consumerGroup, eventId)) {
            return LeaderboardProcessingOutcome.DUPLICATE;
        }
        long delta = resolveDelta(envelope);
        persistProcessedEvent(consumerGroup, envelope);
        leaderboardScoreRepository.ensureScoreRow(envelope.playerId());
        LeaderboardScore score = leaderboardScoreRepository.findByPlayerIdForUpdate(envelope.playerId()).orElseThrow();
        long nextScore = Math.max(0L, score.getScore() + delta);
        score.setScore(nextScore);
        leaderboardScoreRepository.save(score);
        return LeaderboardProcessingOutcome.PROCESSED;
    }

    private void validateEnvelope(PlayerActivityEventEnvelope envelope) {
        requireNonBlank("eventId", envelope.eventId());
        requireNonBlank("eventType", envelope.eventType());
        requireNonBlank("playerId", envelope.playerId());
        if (envelope.payload() == null) {
            throw new InvalidLeaderboardEventException("payload is null");
        }
        if (!SUPPORTED_EVENT_TYPES.contains(envelope.eventType())) {
            throw new InvalidLeaderboardEventException("unsupported eventType");
        }
    }

    private void persistProcessedEvent(String consumerGroup, PlayerActivityEventEnvelope envelope) {
        LeaderboardProcessedEvent event = new LeaderboardProcessedEvent();
        event.setConsumerGroup(consumerGroup);
        event.setEventId(envelope.eventId());
        event.setEventType(envelope.eventType());
        event.setPlayerId(envelope.playerId());
        try {
            processedEventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException duplicate) {
            if (processedEventRepository.existsByConsumerGroupAndEventId(consumerGroup, envelope.eventId())) {
                throw new DuplicateLeaderboardEventException(duplicate);
            }
            throw duplicate;
        }
    }

    private long resolveDelta(PlayerActivityEventEnvelope envelope) {
        return switch (envelope.eventType()) {
            case PlayerActivityEventType.WALLET_CREDITED_V1 -> readPositiveDelta(envelope, "amount");
            case PlayerActivityEventType.WALLET_DEBITED_V1 -> -readPositiveDelta(envelope, "amount");
            case PlayerActivityEventType.REWARD_CLAIMED_V1 -> readPositiveDelta(envelope, "rewardAmount");
            default -> throw new InvalidLeaderboardEventException("unsupported eventType");
        };
    }

    private long readPositiveDelta(PlayerActivityEventEnvelope envelope, String field) {
        Object raw = envelope.payload().get(field);
        if (!(raw instanceof Number numericValue)) {
            throw new InvalidLeaderboardEventException("invalid payload field: " + field);
        }
        double asDouble = numericValue.doubleValue();
        if (!Double.isFinite(asDouble) || Math.floor(asDouble) != asDouble) {
            throw new InvalidLeaderboardEventException("non-integer payload field: " + field);
        }
        long value = numericValue.longValue();
        if (value <= 0 || value > properties.getMaxScoreDelta()) {
            throw new InvalidLeaderboardEventException("out-of-range payload field: " + field);
        }
        return value;
    }

    private void requireNonBlank(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidLeaderboardEventException(field + " is blank");
        }
    }
}
