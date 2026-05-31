package com.game.backend.game.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaPlayerActivityPublisherWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
        .withBean(KafkaPlayerActivityPublisher.class);

    @Test
    void context_wiresKafkaPlayerActivityPublisherWithKafkaTemplate() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(KafkaPlayerActivityPublisher.class);
            assertThat(context).hasSingleBean(PlayerActivityPublisher.class);
            assertThat(context).hasSingleBean(KafkaTemplate.class);
        });
    }
}
