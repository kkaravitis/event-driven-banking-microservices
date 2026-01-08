package com.wordpress.kkaravitis.banking.outbox;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodic cleanup job for the outbox_message table.
 */
@RequiredArgsConstructor
public class OutboxCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupJob.class);

    private final OutboxMessageRepository repository;
    private final OutboxCleanupProperties cleanupProperties;

    @Scheduled(cron = "${cron.delete.old.outbox.messages}")
    @SchedulerLock(
        name = "delete_old_outbox_messages",
        lockAtMostFor = "${outbox.cleanup.lock-at-most-for:PT5M}",
        lockAtLeastFor = "${outbox.cleanup.lock-at-least-for:PT10S}"
    )
    @Transactional
    public void deleteOldMessages() {
        Instant threshold = Instant.now().minus(cleanupProperties.retention());
        int deleted = repository.deleteOlderThan(threshold);
        log.debug("Deleted {} outbox messages older than {}", deleted, threshold);
    }
}
