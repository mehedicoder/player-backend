package com.game.backend.game.repository;

import com.game.backend.game.domain.NotificationJob;
import com.game.backend.game.domain.NotificationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Repository for durable notification jobs.
 */
public interface NotificationJobRepository extends JpaRepository<NotificationJob, Long> {

    /**
     * Returns due job ids using MySQL skip-locked semantics for multi-worker polling.
     */
    @Query(value = """
        select id from notification_job
        where status in ('PENDING', 'RETRY_WAIT')
          and next_attempt_at <= :now
        order by next_attempt_at asc, id asc
        limit :limit
        for update skip locked
        """, nativeQuery = true)
    List<Long> findDueJobIdsForClaim(@Param("now") OffsetDateTime now, @Param("limit") int limit);

    /**
     * Marks a due job as processing only if it is still claimable.
     */
    @Modifying
    @Query("""
        update NotificationJob j
        set j.status = com.game.backend.game.domain.NotificationJobStatus.PROCESSING,
            j.attemptCount = j.attemptCount + 1,
            j.claimToken = :claimToken,
            j.lastError = null,
            j.updatedAt = :now
        where j.id = :id
          and j.status in :statuses
          and j.nextAttemptAt <= :now
          and j.attemptCount < :maxAttempts
        """)
    int markProcessing(
        @Param("id") Long id,
        @Param("statuses") Collection<NotificationJobStatus> statuses,
        @Param("now") OffsetDateTime now,
        @Param("claimToken") String claimToken,
        @Param("maxAttempts") int maxAttempts
    );

    /**
     * Marks stale processing jobs as permanently failed to avoid unsafe duplicate dispatch.
     */
    @Modifying
    @Query("""
        update NotificationJob j
        set j.status = com.game.backend.game.domain.NotificationJobStatus.FAILED_PERMANENT,
            j.lastError = 'processing_timeout',
            j.claimToken = null,
            j.updatedAt = :now
        where j.status = com.game.backend.game.domain.NotificationJobStatus.PROCESSING
          and j.updatedAt <= :staleBefore
        """)
    int failStaleProcessingJobs(
        @Param("now") OffsetDateTime now,
        @Param("staleBefore") OffsetDateTime staleBefore
    );

    /**
     * Marks a claimed job as sent if the same worker still owns the claim.
     */
    @Modifying
    @Query("""
        update NotificationJob j
        set j.status = com.game.backend.game.domain.NotificationJobStatus.SENT,
            j.lastError = null,
            j.claimToken = null,
            j.updatedAt = :now
        where j.id = :id
          and j.status = com.game.backend.game.domain.NotificationJobStatus.PROCESSING
          and j.claimToken = :claimToken
        """)
    int markSentIfClaimed(
        @Param("id") Long id,
        @Param("claimToken") String claimToken,
        @Param("now") OffsetDateTime now
    );

    /**
     * Schedules a claimed job for retry if attempts remain.
     */
    @Modifying
    @Query("""
        update NotificationJob j
        set j.status = com.game.backend.game.domain.NotificationJobStatus.RETRY_WAIT,
            j.nextAttemptAt = :nextAttemptAt,
            j.lastError = :lastError,
            j.claimToken = null,
            j.updatedAt = :now
        where j.id = :id
          and j.status = com.game.backend.game.domain.NotificationJobStatus.PROCESSING
          and j.claimToken = :claimToken
          and j.attemptCount < :maxAttempts
        """)
    int markRetryWaitIfClaimed(
        @Param("id") Long id,
        @Param("claimToken") String claimToken,
        @Param("nextAttemptAt") OffsetDateTime nextAttemptAt,
        @Param("lastError") String lastError,
        @Param("now") OffsetDateTime now,
        @Param("maxAttempts") int maxAttempts
    );

    /**
     * Marks a claimed job as permanently failed when attempts are exhausted.
     */
    @Modifying
    @Query("""
        update NotificationJob j
        set j.status = com.game.backend.game.domain.NotificationJobStatus.FAILED_PERMANENT,
            j.lastError = :lastError,
            j.claimToken = null,
            j.updatedAt = :now
        where j.id = :id
          and j.status = com.game.backend.game.domain.NotificationJobStatus.PROCESSING
          and j.claimToken = :claimToken
        """)
    int markFailedPermanentIfClaimed(
        @Param("id") Long id,
        @Param("claimToken") String claimToken,
        @Param("lastError") String lastError,
        @Param("now") OffsetDateTime now
    );

    /**
     * Counts jobs for one source event.
     */
    long countByEventId(String eventId);

    /**
     * Finds jobs for one source event.
     */
    List<NotificationJob> findByEventId(String eventId);
}
