package com.game.backend.game.service;

import com.game.backend.game.domain.NotificationJob;
import com.game.backend.game.domain.NotificationJobStatus;
import com.game.backend.game.repository.NotificationJobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Polls durable notification jobs and applies bounded retry semantics.
 */
@Service
public class NotificationWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationWorker.class);
    private static final Set<NotificationJobStatus> DUE_STATUSES = Set.of(
        NotificationJobStatus.PENDING,
        NotificationJobStatus.RETRY_WAIT
    );

    private final NotificationJobRepository notificationJobRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final NotificationWorkerProperties properties;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;

    public NotificationWorker(
        NotificationJobRepository notificationJobRepository,
        NotificationDispatcher notificationDispatcher,
        NotificationWorkerProperties properties,
        MeterRegistry meterRegistry,
        TransactionTemplate transactionTemplate
    ) {
        this.notificationJobRepository = notificationJobRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Claims and processes a bounded batch of due jobs without holding DB locks during dispatch.
     */
    public int processDueBatch() {
        List<NotificationJob> jobs = claimDueJobs();
        for (NotificationJob job : jobs) {
            processJob(job);
        }
        return jobs.size();
    }

    private List<NotificationJob> claimDueJobs() {
        return transactionTemplate.execute(status -> {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime staleBefore = now.minusSeconds(properties.getProcessingTimeoutSeconds());
            int timedOut = notificationJobRepository.failStaleProcessingJobs(now, staleBefore);
            if (timedOut > 0) {
                meterRegistry.counter("notification.jobs.failed_permanent").increment(timedOut);
                log.error("notification_jobs_processing_timeout count={}", timedOut);
            }
            List<Long> jobIds = notificationJobRepository.findDueJobIdsForClaim(now, properties.getBatchSize());
            List<NotificationJob> claimed = new ArrayList<>();
            for (Long jobId : jobIds) {
                String claimToken = UUID.randomUUID().toString();
                int updated = notificationJobRepository.markProcessing(
                    jobId,
                    DUE_STATUSES,
                    now,
                    claimToken,
                    properties.getMaxAttempts()
                );
                if (updated == 1) {
                    notificationJobRepository.findById(jobId).ifPresent(claimed::add);
                }
            }
            return claimed;
        });
    }

    private void processJob(NotificationJob job) {
        try {
            notificationDispatcher.dispatch(job);
            markSent(job);
        } catch (RuntimeException dispatchFailure) {
            handleDispatchFailure(job, dispatchFailure);
        }
    }

    private void markSent(NotificationJob claimedJob) {
        transactionTemplate.executeWithoutResult(status -> {
            int updated = notificationJobRepository.markSentIfClaimed(
                claimedJob.getId(),
                claimedJob.getClaimToken(),
                OffsetDateTime.now(ZoneOffset.UTC)
            );
            if (updated == 1) {
                meterRegistry.counter("notification.jobs.sent").increment();
            } else {
                log.warn(
                    "notification_job_claim_lost jobId={} eventId={} outcome=sent",
                    claimedJob.getId(),
                    sanitize(claimedJob.getEventId())
                );
            }
        });
    }

    private void handleDispatchFailure(NotificationJob claimedJob, RuntimeException dispatchFailure) {
        transactionTemplate.executeWithoutResult(status -> {
            applyDispatchFailure(claimedJob, dispatchFailure);
        });
    }

    private void applyDispatchFailure(NotificationJob job, RuntimeException dispatchFailure) {
        String error = sanitizeError(dispatchFailure);
        if (job.getAttemptCount() >= properties.getMaxAttempts()) {
            int updated = notificationJobRepository.markFailedPermanentIfClaimed(
                job.getId(),
                job.getClaimToken(),
                error,
                OffsetDateTime.now(ZoneOffset.UTC)
            );
            if (updated == 1) {
                meterRegistry.counter("notification.jobs.failed_permanent").increment();
                log.error(
                    "notification_job_failed_permanent jobId={} eventId={} attempts={} reason={}",
                    job.getId(),
                    sanitize(job.getEventId()),
                    job.getAttemptCount(),
                    error
                );
            }
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime nextAttemptAt = now.plusSeconds(backoffSeconds(job.getAttemptCount()));
        int updated = notificationJobRepository.markRetryWaitIfClaimed(
            job.getId(),
            job.getClaimToken(),
            nextAttemptAt,
            error,
            now,
            properties.getMaxAttempts()
        );
        if (updated == 1) {
            meterRegistry.counter("notification.jobs.retry").increment();
            log.warn(
                "notification_job_retry_wait jobId={} eventId={} attempts={} reason={}",
                job.getId(),
                sanitize(job.getEventId()),
                job.getAttemptCount(),
                error
            );
        } else {
            log.warn(
                "notification_job_claim_lost jobId={} eventId={} outcome=retry reason={}",
                job.getId(),
                sanitize(job.getEventId()),
                error
            );
        }
    }

    private long backoffSeconds(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> 1L;
            case 2 -> 2L;
            default -> 4L;
        };
    }

    private String sanitizeError(RuntimeException dispatchFailure) {
        String message = dispatchFailure.getMessage();
        if (message == null || message.isBlank()) {
            message = dispatchFailure.getClass().getSimpleName();
        }
        return sanitize(message);
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String sanitized = value.replace('\n', '_').replace('\r', '_').replace('\t', '_');
        return sanitized.length() > 512 ? sanitized.substring(0, 512) : sanitized;
    }
}
