package com.wordpress.kkaravitis.banking.idempotency.inbox;

import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration that registers the inbox idempotency infrastructure:
 * entity, repository scanning, and {@link InboxService}.
 */
@AutoConfiguration
@ConditionalOnClass({EntityManager.class})
@ConditionalOnProperty(prefix = "idempotency.inbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(InboxCleanupProperties.class)
public class InboxIdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InboxService inboxService(InboxMessageRepository repository) {
        return new InboxService(repository);
    }
}
