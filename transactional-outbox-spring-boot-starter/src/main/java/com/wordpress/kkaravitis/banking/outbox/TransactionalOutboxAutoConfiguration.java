package com.wordpress.kkaravitis.banking.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Core auto-configuration: entity + repository + TransactionalOutbox bean.
 */
@AutoConfiguration
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(prefix = "outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({OutboxCleanupProperties.class})
//@EntityScan(basePackageClasses = OutboxMessage.class)
//@EnableJpaRepositories(basePackageClasses = OutboxMessageRepository.class)
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
