package com.game.backend.config;

import com.game.backend.game.service.InvalidMatchResultEventException;
import com.game.backend.game.service.MatchResultConsumerProperties;
import com.game.backend.game.service.MatchResultDlqPublisher;
import com.game.backend.game.service.NotificationWorkerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer wiring for match-result ingestion and notification scheduling.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({MatchResultConsumerProperties.class, NotificationWorkerProperties.class})
public class MatchResultKafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(MatchResultKafkaConsumerConfig.class);

    /**
     * Creates consumer factory with explicit string deserialization.
     */
    @Bean
    ConsumerFactory<String, String> matchResultConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates listener factory with manual ack and retry strategy.
     */
    @Bean(name = "matchResultKafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> matchResultKafkaListenerContainerFactory(
        @Qualifier("matchResultConsumerFactory") ConsumerFactory<String, String> matchResultConsumerFactory,
        DefaultErrorHandler matchResultConsumerErrorHandler,
        MatchResultConsumerProperties properties
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(matchResultConsumerFactory);
        factory.setCommonErrorHandler(matchResultConsumerErrorHandler);
        factory.setConcurrency(properties.getConcurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    /**
     * Configures retry, DLQ recovery, and non-retriable exception classification.
     */
    @Bean
    DefaultErrorHandler matchResultConsumerErrorHandler(
        MatchResultConsumerProperties properties,
        MatchResultDlqPublisher dlqPublisher,
        MeterRegistry meterRegistry
    ) {
        ExponentialBackOffWithMaxRetries backOff =
            new ExponentialBackOffWithMaxRetries(properties.getRetryMaxAttempts() - 1);
        backOff.setInitialInterval(properties.getRetryInitialIntervalMs());
        backOff.setMultiplier(2.0d);
        backOff.setMaxInterval(properties.getRetryMaxIntervalMs());
        DefaultErrorHandler handler = new DefaultErrorHandler((record, ex) -> {
            dlqPublisher.publish(record, ex);
        }, backOff);
        handler.defaultFalse();
        handler.addRetryableExceptions(
            TransientDataAccessException.class,
            CannotCreateTransactionException.class,
            TransactionSystemException.class,
            RetriableException.class
        );
        handler.addNotRetryableExceptions(InvalidMatchResultEventException.class);
        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            meterRegistry.counter("match_result.events.retried").increment();
            log.warn(
                "match_result_consumer_retry topic={} partition={} offset={} attempt={} reason={}",
                record.topic(),
                record.partition(),
                record.offset(),
                deliveryAttempt,
                ex.getClass().getSimpleName()
            );
        });
        handler.setCommitRecovered(true);
        handler.setAckAfterHandle(true);
        return handler;
    }
}
