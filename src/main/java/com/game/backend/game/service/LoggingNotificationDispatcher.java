package com.game.backend.game.service;

import com.game.backend.game.domain.NotificationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Local notification dispatcher placeholder for v1 durable worker semantics.
 */
@Component
public class LoggingNotificationDispatcher implements NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationDispatcher.class);

    /**
     * Logs safe notification identifiers instead of calling an external provider.
     */
    @Override
    public void dispatch(NotificationJob job) {
        log.info(
            "notification_dispatched jobId={} eventId={} type={}",
            job.getId(),
            sanitize(job.getEventId()),
            sanitize(job.getNotificationType())
        );
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String sanitized = value.replace('\n', '_').replace('\r', '_').replace('\t', '_');
        return sanitized.length() > 128 ? sanitized.substring(0, 128) : sanitized;
    }
}
