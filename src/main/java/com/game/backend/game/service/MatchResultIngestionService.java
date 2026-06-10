package com.game.backend.game.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.backend.game.domain.MatchResultType;
import com.game.backend.game.domain.NotificationJob;
import com.game.backend.game.repository.MatchResultProcessedEventRepository;
import com.game.backend.game.repository.MatchResultProjectionRepository;
import com.game.backend.game.repository.NotificationJobRepository;
import com.game.backend.player.repository.PlayerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Applies match-result events idempotently and creates notification jobs atomically.
 */
@Service
public class MatchResultIngestionService {

    private static final String NOTIFICATION_TYPE = "MATCH_RESULT_RECORDED";

    private final MatchResultProjectionRepository projectionRepository;
    private final MatchResultProcessedEventRepository processedEventRepository;
    private final NotificationJobRepository notificationJobRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final MatchResultConsumerProperties properties;
    private final ObjectMapper objectMapper;

    public MatchResultIngestionService(
        MatchResultProjectionRepository projectionRepository,
        MatchResultProcessedEventRepository processedEventRepository,
        NotificationJobRepository notificationJobRepository,
        PlayerProfileRepository playerProfileRepository,
        MatchResultConsumerProperties properties,
        ObjectMapper objectMapper
    ) {
        this.projectionRepository = projectionRepository;
        this.processedEventRepository = processedEventRepository;
        this.notificationJobRepository = notificationJobRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Processes one event in a single database transaction.
     */
    @Transactional
    public MatchResultProcessingOutcome processEvent(MatchResultEventEnvelope envelope, String consumerGroup) {
        ValidatedMatchResultEvent event = validate(envelope);
        if (processedEventRepository.existsByConsumerGroupAndEventId(consumerGroup, event.eventId())) {
            return MatchResultProcessingOutcome.DUPLICATE;
        }
        if (!persistProcessedEvent(consumerGroup, event)) {
            return MatchResultProcessingOutcome.DUPLICATE;
        }
        if (upsertProjection(event)) {
            createNotificationJob(event);
        }
        return MatchResultProcessingOutcome.PROCESSED;
    }

    private ValidatedMatchResultEvent validate(MatchResultEventEnvelope envelope) {
        if (envelope == null) {
            throw new InvalidMatchResultEventException("envelope is null");
        }
        String eventId = requireNonBlank("eventId", envelope.eventId(), 128);
        String eventType = requireNonBlank("eventType", envelope.eventType(), 64);
        String playerId = requireNonBlank("playerId", envelope.playerId(), 64);
        if (!MatchResultEventType.MATCH_RESULT_RECORDED_V1.equals(eventType)) {
            throw new InvalidMatchResultEventException("unsupported eventType");
        }
        if (!playerProfileRepository.existsById(playerId)) {
            throw new InvalidMatchResultEventException("player does not exist");
        }
        if (envelope.payload() == null) {
            throw new InvalidMatchResultEventException("payload is null");
        }
        OffsetDateTime occurredAt = parseOccurredAt(envelope.occurredAt());
        String matchId = requirePayloadString(envelope.payload(), "matchId", 128);
        MatchResultType result = parseResult(envelope.payload().get("result"));
        int scoreDelta = parseScoreDelta(envelope.payload().get("scoreDelta"));
        return new ValidatedMatchResultEvent(eventId, eventType, playerId, occurredAt, matchId, result, scoreDelta);
    }

    private boolean persistProcessedEvent(String consumerGroup, ValidatedMatchResultEvent event) {
        int inserted = processedEventRepository.insertIgnoreProcessedEvent(
            consumerGroup,
            event.eventId(),
            event.eventType(),
            event.playerId()
        );
        if (inserted == 1) {
            return true;
        }
        if (processedEventRepository.existsByConsumerGroupAndEventId(consumerGroup, event.eventId())) {
            return false;
        }
        throw new InvalidMatchResultEventException("data integrity violation");
    }

    private boolean upsertProjection(ValidatedMatchResultEvent event) {
        int affectedRows = projectionRepository.upsertIfFresh(
            event.playerId(),
            event.matchId(),
            event.result().name(),
            event.scoreDelta(),
            event.eventId(),
            event.occurredAt()
        );
        return affectedRows > 0;
    }

    private void createNotificationJob(ValidatedMatchResultEvent event) {
        NotificationJob job = new NotificationJob();
        job.setEventId(event.eventId());
        job.setPlayerId(event.playerId());
        job.setNotificationType(NOTIFICATION_TYPE);
        job.setPayloadJson(toJson(Map.of(
            "eventId", event.eventId(),
            "playerId", event.playerId(),
            "matchId", event.matchId(),
            "result", event.result().name(),
            "scoreDelta", event.scoreDelta()
        )));
        notificationJobRepository.save(job);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException serializationError) {
            throw new IllegalStateException("failed to serialize notification payload", serializationError);
        }
    }

    private OffsetDateTime parseOccurredAt(String raw) {
        String value = requireNonBlank("occurredAt", raw, 64);
        try {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException parseFailure) {
            throw new InvalidMatchResultEventException("occurredAt is invalid");
        }
    }

    private String requirePayloadString(Map<String, Object> payload, String fieldName, int maxLength) {
        Object raw = payload.get(fieldName);
        if (!(raw instanceof String value)) {
            throw new InvalidMatchResultEventException("invalid payload field: " + fieldName);
        }
        return requireNonBlank(fieldName, value, maxLength);
    }

    private MatchResultType parseResult(Object raw) {
        if (!(raw instanceof String value)) {
            throw new InvalidMatchResultEventException("invalid payload field: result");
        }
        try {
            return MatchResultType.valueOf(value);
        } catch (IllegalArgumentException invalidResult) {
            throw new InvalidMatchResultEventException("invalid payload field: result");
        }
    }

    private int parseScoreDelta(Object raw) {
        if (!(raw instanceof Number numericValue)) {
            throw new InvalidMatchResultEventException("invalid payload field: scoreDelta");
        }
        double asDouble = numericValue.doubleValue();
        if (!Double.isFinite(asDouble) || Math.floor(asDouble) != asDouble) {
            throw new InvalidMatchResultEventException("non-integer payload field: scoreDelta");
        }
        long scoreDelta = numericValue.longValue();
        int maxDelta = properties.getMaxScoreDelta();
        if (scoreDelta < -maxDelta || scoreDelta > maxDelta) {
            throw new InvalidMatchResultEventException("out-of-range payload field: scoreDelta");
        }
        return (int) scoreDelta;
    }

    private String requireNonBlank(String fieldName, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidMatchResultEventException(fieldName + " is blank");
        }
        if (value.length() > maxLength) {
            throw new InvalidMatchResultEventException(fieldName + " is too long");
        }
        return value;
    }

    private record ValidatedMatchResultEvent(
        String eventId,
        String eventType,
        String playerId,
        OffsetDateTime occurredAt,
        String matchId,
        MatchResultType result,
        int scoreDelta
    ) {
    }
}
