package com.wordpress.kkaravitis.banking.idempotency.inbox;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration that enables the periodic cleanup job.
 */
@AutoConfiguration(after = InboxIdempotencyAutoConfiguration.class)
@ConditionalOnProperty(prefix = "idempotency.inbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "inbox.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = "cron.delete.old.inbox")
@ConditionalOnClass({EnableScheduling.class, LockProvider.class})
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "${scheduler.lock-at-most-for:PT10M}")
public class InboxIdempotencySchedulingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockProvider.class)
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public InboxCleanupJob inboxCleanupJob(InboxMessageRepository repository, InboxCleanupProperties cleanupProperties) {
        return new InboxCleanupJob(repository, cleanupProperties);
    }
}
