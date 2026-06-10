package com.game.backend.game.service;

/**
 * Kafka topic constants for match-result processing.
 */
public final class MatchResultTopics {

    public static final String MATCH_RESULTS_V1 = "match-results.v1";
    public static final String MATCH_RESULTS_V1_DLQ = "match-results.v1.dlq";

    private MatchResultTopics() {
    }
}
