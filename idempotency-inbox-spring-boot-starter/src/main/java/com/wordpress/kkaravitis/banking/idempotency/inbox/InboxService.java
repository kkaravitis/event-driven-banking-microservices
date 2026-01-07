package com.wordpress.kkaravitis.banking.idempotency.inbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inbox-based idempotency service.
 *
 * <p>Typical usage (e.g., in a message consumer):
 * <pre>
 * if (!inboxService.validateAndStore(messageId)) {
 *     return; // duplicate
 * }
 * // process
 * </pre>
 */
public class InboxService {

    private static final Logger log = LoggerFactory.getLogger(InboxService.class);

    private final InboxMessageRepository repository;

    public InboxService(InboxMessageRepository repository) {
        this.repository = repository;
    }

    /**
     * @return true if {@code messageId} was not present and has been stored now; false if it's a duplicate.
     */
    @Transactional
    public boolean validateAndStore(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            log.error("Empty message id detected");
            return false;
        }

        // Rely on the DB UNIQUE(message_id) constraint to be race-safe across replicas.
        try {
            repository.save(new InboxMessage(messageId));
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("duplicated inbox message detected, id: {}", messageId);
            return false;
        }
    }
}
