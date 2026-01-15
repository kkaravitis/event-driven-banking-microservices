# Idempotency Inbox Spring Boot Starter

This starter provides an **inbox table** + a small service to implement **message idempotency** (deduplicate by `messageId`) across microservices.

## What you get

- `InboxMessage` JPA entity (`inbox_message` table)
- `InboxMessageRepository`
- `InboxService#validateAndStore(messageId)`
- Optional scheduled cleanup job (deletes old inbox rows)
- ShedLock JDBC lock provider + migration for the `shedlock` table

## Database / Flyway

The starter ships two migrations under `classpath:db/migration`:

- `V9000__idempotency_create_inbox_message.sql`
- `V9001__idempotency_create_shedlock_table.sql`

> If your service already uses high Flyway versions near `9000`, rename these migrations in your fork (or move them to a dedicated Flyway location).

## Usage

### 1) Add dependency

```xml
<dependency>
  <groupId>com.wordpress.kkaravitis.banking</groupId>
  <artifactId>idempotency-inbox-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2) Configure

```yaml
idempotency:
  inbox:
    enabled: true

# cleanup is enabled by default if the cron is present
cron:
  delete:
    old:
      messages: "0 0/10 * * * *"   # every 10 minutes

inbox:
  cleanup:
    enabled: true
    retention: "P1D"              # keep 1 day
    lock-at-most-for: "PT5M"
    lock-at-least-for: "PT10S"

scheduler:
  lock-at-most-for: "PT10M"       # default for @EnableSchedulerLock

spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
```

### 3) Call it in your consumer

```java
if (!inboxService.validateAndStore(messageId)) {
    return; // duplicate
}

// process message
```

## Disabling

- Disable everything:
  - `idempotency.inbox.enabled=false`
- Disable cleanup job only:
  - `inbox.cleanup.enabled=false` (or remove `cron.delete.old.inbox`)
