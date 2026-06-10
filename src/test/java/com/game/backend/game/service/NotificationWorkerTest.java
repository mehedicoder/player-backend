package com.game.backend.game.service;

import com.game.backend.game.domain.NotificationJob;
import com.game.backend.game.domain.NotificationJobStatus;
import com.game.backend.game.repository.NotificationJobRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class NotificationWorkerTest {

    @Mock
    private NotificationJobRepository notificationJobRepository;
    @Mock
    private NotificationDispatcher notificationDispatcher;

    private NotificationWorker worker;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        NotificationWorkerProperties properties = new NotificationWorkerProperties();
        meterRegistry = new SimpleMeterRegistry();
        worker = new NotificationWorker(
            notificationJobRepository,
            notificationDispatcher,
            properties,
            meterRegistry,
            new TransactionTemplate(transactionManager())
        );
    }

    @Test
    void processDueBatch_success_marksJobSent() {
        NotificationJob job = job(1);
        stubClaim(job);
        when(notificationJobRepository.markSentIfClaimed(eq(1L), any(String.class), any(OffsetDateTime.class)))
            .thenReturn(1);

        int processed = worker.processDueBatch();

        assertThat(processed).isEqualTo(1);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(meterRegistry.counter("notification.jobs.sent").count()).isEqualTo(1.0d);
        verify(notificationDispatcher).dispatch(job);
        verify(notificationJobRepository).markSentIfClaimed(eq(1L), eq(job.getClaimToken()), any(OffsetDateTime.class));
    }

    @Test
    void processDueBatch_dispatchFailureBeforeMaxAttempts_setsRetryWait() {
        NotificationJob job = job(1);
        stubClaim(job);
        when(notificationJobRepository.markRetryWaitIfClaimed(
            eq(1L),
            any(String.class),
            any(OffsetDateTime.class),
            eq("provider_down"),
            any(OffsetDateTime.class),
            eq(4)
        )).thenReturn(1);
        doThrow(new RuntimeException("provider_down")).when(notificationDispatcher).dispatch(job);

        worker.processDueBatch();

        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(meterRegistry.counter("notification.jobs.retry").count()).isEqualTo(1.0d);
        verify(notificationJobRepository).markRetryWaitIfClaimed(
            eq(1L),
            eq(job.getClaimToken()),
            any(OffsetDateTime.class),
            eq("provider_down"),
            any(OffsetDateTime.class),
            eq(4)
        );
    }

    @Test
    void processDueBatch_dispatchFailureAtMaxAttempts_marksFailedPermanent() {
        NotificationJob job = job(4);
        stubClaim(job);
        when(notificationJobRepository.markFailedPermanentIfClaimed(
            eq(1L),
            any(String.class),
            eq("provider_down"),
            any(OffsetDateTime.class)
        )).thenReturn(1);
        doThrow(new RuntimeException("provider_down")).when(notificationDispatcher).dispatch(job);

        worker.processDueBatch();

        assertThat(job.getAttemptCount()).isEqualTo(4);
        assertThat(meterRegistry.counter("notification.jobs.failed_permanent").count()).isEqualTo(1.0d);
        verify(notificationJobRepository).markFailedPermanentIfClaimed(
            eq(1L),
            eq(job.getClaimToken()),
            eq("provider_down"),
            any(OffsetDateTime.class)
        );
    }

    @Test
    void processDueBatch_claimsJobBeforeDispatch() {
        NotificationJob job = job(1);
        stubClaim(job);

        worker.processDueBatch();

        verify(notificationJobRepository).failStaleProcessingJobs(any(OffsetDateTime.class), any(OffsetDateTime.class));
        verify(notificationJobRepository).findDueJobIdsForClaim(any(OffsetDateTime.class), anyInt());
        verify(notificationJobRepository).markProcessing(eq(1L), anyCollection(), any(OffsetDateTime.class), any(String.class), eq(4));
    }

    @Test
    void processDueBatch_staleProcessingJobs_areFailedPermanentWithoutDispatch() {
        when(notificationJobRepository.failStaleProcessingJobs(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(2);
        when(notificationJobRepository.findDueJobIdsForClaim(any(OffsetDateTime.class), anyInt()))
            .thenReturn(List.of());

        int processed = worker.processDueBatch();

        assertThat(processed).isZero();
        assertThat(meterRegistry.counter("notification.jobs.failed_permanent").count()).isEqualTo(2.0d);
        verify(notificationDispatcher, never()).dispatch(any());
    }

    @Test
    void processDueBatch_lostClaimDoesNotCountSent() {
        NotificationJob job = job(1);
        stubClaim(job);
        when(notificationJobRepository.markSentIfClaimed(eq(1L), any(String.class), any(OffsetDateTime.class)))
            .thenReturn(0);

        worker.processDueBatch();

        assertThat(meterRegistry.counter("notification.jobs.sent").count()).isZero();
    }

    private NotificationJob job(int attempts) {
        NotificationJob job = new NotificationJob();
        ReflectionTestUtils.setField(job, "id", 1L);
        job.setEventId("event-1");
        job.setPlayerId("player-1");
        job.setNotificationType("MATCH_RESULT_RECORDED");
        job.setPayloadJson("{}");
        job.setAttemptCount(attempts);
        job.setStatus(NotificationJobStatus.PENDING);
        job.setNextAttemptAt(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        return job;
    }

    private void stubClaim(NotificationJob job) {
        when(notificationJobRepository.failStaleProcessingJobs(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(0);
        when(notificationJobRepository.findDueJobIdsForClaim(any(OffsetDateTime.class), anyInt()))
            .thenReturn(List.of(1L));
        when(notificationJobRepository.markProcessing(eq(1L), anyCollection(), any(OffsetDateTime.class), any(String.class), eq(4)))
            .thenAnswer(invocation -> {
                job.setClaimToken(invocation.getArgument(3));
                return 1;
            });
        when(notificationJobRepository.findById(1L))
            .thenReturn(Optional.of(job));
    }

    private PlatformTransactionManager transactionManager() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
    }
}
