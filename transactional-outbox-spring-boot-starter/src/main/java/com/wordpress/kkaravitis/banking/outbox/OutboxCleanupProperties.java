package com.wordpress.kkaravitis.banking.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox.cleanup")
public record OutboxCleanupProperties(
    Duration retention,
    Duration lockAtMostFor,
    Duration lockAtLeastFor,
    Boolean enabled
) {
}
