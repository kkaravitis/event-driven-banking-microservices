package com.wordpress.kkaravitis.banking.idempotency.inbox;

import java.time.Instant;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodic cleanup job for the inbox_message table.
 */
public class InboxCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(InboxCleanupJob.class);

    private final InboxMessageRepository repository;
    private final InboxCleanupProperties cleanupProperties;

    public InboxCleanupJob(InboxMessageRepository repository, InboxCleanupProperties cleanupProperties) {
        this.repository = repository;
        this.cleanupProperties = cleanupProperties;
    }

    @Scheduled(cron = "${cron.delete.old.inbox}")
    @SchedulerLock(
        name = "delete_old_inbox_messages",
        lockAtMostFor = "${inbox.cleanup.lock-at-most-for:PT5M}",
        lockAtLeastFor = "${inbox.cleanup.lock-at-least-for:PT10S}"
    )
    @Transactional
    public void deleteOldMessages() {
        Instant threshold = Instant.now().minus(cleanupProperties.retention());
        int deleted = repository.deleteOlderThan(threshold);
        log.debug("Deleted {} inbox messages older than {}", deleted, threshold);
    }
}
