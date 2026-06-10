package com.game.backend.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchResultDlqPublisherTest {

    @Test
    void publish_buildsRequiredDlqMetadata() throws Exception {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(eq(MatchResultTopics.MATCH_RESULTS_V1_DLQ), eq("player-1"), any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        ObjectMapper objectMapper = new ObjectMapper();
        MatchResultConsumerProperties properties = new MatchResultConsumerProperties();
        MatchResultDlqPublisher publisher = new MatchResultDlqPublisher(
            kafkaTemplate,
            objectMapper,
            properties,
            new SimpleMeterRegistry()
        );
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            MatchResultTopics.MATCH_RESULTS_V1,
            2,
            42L,
            "player-1",
            "{\"eventId\":\"event-1\",\"eventType\":\"match.result.recorded.v1\"}"
        );

        publisher.publish(record, new RuntimeException("db_down"));

        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(kafkaTemplate).send(eq(MatchResultTopics.MATCH_RESULTS_V1_DLQ), eq("player-1"), payload.capture());
        Map<String, Object> parsedPayload = payload.getValue();
        assertThat(parsedPayload).containsEntry("originalTopic", MatchResultTopics.MATCH_RESULTS_V1);
        assertThat(parsedPayload).containsEntry("originalPartition", 2);
        assertThat(parsedPayload).containsEntry("originalOffset", 42L);
        assertThat(parsedPayload).containsEntry("eventId", "event-1");
        assertThat(parsedPayload).containsEntry("eventType", MatchResultEventType.MATCH_RESULT_RECORDED_V1);
        assertThat(parsedPayload).containsEntry("consumerGroup", "match-result-consumer-v1");
        assertThat(parsedPayload).containsEntry("errorCategory", "TRANSIENT_RETRY_EXHAUSTED");
        assertThat(parsedPayload).containsKey("failedAt");
        assertThat(parsedPayload).doesNotContainKey("originalPayload");
    }
}
