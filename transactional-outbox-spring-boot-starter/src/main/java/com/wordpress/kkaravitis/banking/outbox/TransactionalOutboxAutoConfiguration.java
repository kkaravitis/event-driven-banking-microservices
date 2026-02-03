package com.wordpress.kkaravitis.banking.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Core auto-configuration: entity + repository + TransactionalOutbox bean.
 */
@AutoConfiguration
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(prefix = "outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({OutboxCleanupProperties.class})
public class TransactionalOutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TransactionalOutbox transactionalOutbox(
        OutboxMessageRepository repository,
        ObjectMapper objectMapper
    ) {
        return new TransactionalOutboxAdapter(repository, objectMapper);
    }
}
