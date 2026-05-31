package com.game.backend.config;

import com.game.backend.game.service.InvalidLeaderboardEventException;
import com.game.backend.game.service.LeaderboardConsumerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer wiring for leaderboard update flow.
 */
@Configuration
@EnableConfigurationProperties(LeaderboardConsumerProperties.class)
public class LeaderboardKafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardKafkaConsumerConfig.class);

    /**
     * Creates consumer factory with explicit string deserialization.
     */
    @Bean
    ConsumerFactory<String, String> leaderboardConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates listener factory with manual ack and retry strategy.
     */
    @Bean(name = "leaderboardKafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> leaderboardKafkaListenerContainerFactory(
        ConsumerFactory<String, String> leaderboardConsumerFactory,
        DefaultErrorHandler leaderboardConsumerErrorHandler,
        LeaderboardConsumerProperties properties
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(leaderboardConsumerFactory);
        factory.setCommonErrorHandler(leaderboardConsumerErrorHandler);
        factory.setConcurrency(properties.getConcurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    /**
     * Configures retry and non-retriable exception classification.
     */
    @Bean
    DefaultErrorHandler leaderboardConsumerErrorHandler(
        LeaderboardConsumerProperties properties,
        MeterRegistry meterRegistry
    ) {
        ExponentialBackOffWithMaxRetries backOff =
            new ExponentialBackOffWithMaxRetries(properties.getRetryMaxAttempts() - 1);
        backOff.setInitialInterval(properties.getRetryInitialIntervalMs());
        backOff.setMultiplier(2.0d);
        backOff.setMaxInterval(properties.getRetryMaxIntervalMs());
        DefaultErrorHandler handler = new DefaultErrorHandler((record, ex) -> {
            meterRegistry.counter("leaderboard.events.retry_exhausted").increment();
            log.error(
                "leaderboard_consumer_retry_exhausted topic={} partition={} offset={} reason={}",
                record.topic(),
                record.partition(),
                record.offset(),
                ex.getClass().getSimpleName()
            );
            throw new RuntimeException("leaderboard consumer retry exhausted", ex);
        }, backOff);
        handler.addNotRetryableExceptions(InvalidLeaderboardEventException.class);
        handler.setCommitRecovered(false);
        handler.setAckAfterHandle(false);
        return handler;
    }
}
