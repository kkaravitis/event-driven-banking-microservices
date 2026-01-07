package com.wordpress.kkaravitis.banking.idempotency.inbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for periodic cleanup of the inbox idempotency table.
 *
 * <p>Example:
 * <pre>
 * inbox.cleanup:
 *   enabled: true
 *   retention: P1D
 *   lock-at-most-for: PT5M
 *   lock-at-least-for: PT10S
 * </pre>
 */
@ConfigurationProperties(prefix = "inbox.cleanup")
public record InboxCleanupProperties(
    Duration retention,
    Duration lockAtMostFor,
    Duration lockAtLeastFor,
    Boolean enabled
) {

    public InboxCleanupProperties {
        if (retention == null) {
            retention = Duration.ofDays(1);
        }
        if (lockAtMostFor == null) {
            lockAtMostFor = Duration.ofMinutes(5);
        }
        if (lockAtLeastFor == null) {
            lockAtLeastFor = Duration.ofSeconds(10);
        }
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }
    }
}
