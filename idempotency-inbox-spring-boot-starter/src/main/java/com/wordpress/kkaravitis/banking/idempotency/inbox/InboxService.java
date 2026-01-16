package com.wordpress.kkaravitis.banking.idempotency.inbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inbox-based idempotency service.
 *
 * <p>Typical usage (e.g., in a transactional method):
 * <pre>
 * if (!inboxService.validateAndStore(messageId)) {
 *     return; // duplicate
 * }
 * // process
 *
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
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean validateAndStore(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            log.error("Empty message id detected");
            return false;
        }

        return repository.insertIfAbsent(messageId) == 1;
    }
}
