# Transactional Outbox Spring Boot Starter

A small Spring Boot starter that provides a **transactional outbox** implementation:

- JPA entity + repository for `outbox_message`
- `TransactionalOutbox` API + default adapter that enqueues messages inside the current transaction
- Optional scheduled cleanup job with ShedLock
- Flyway migrations shipped in the jar under `db/migration`

## What you get

### Beans
- `TransactionalOutbox` (default: `TransactionalOutboxAdapter`)

### Optional scheduled cleanup
Enabled if all of these are true:
- `outbox.enabled=true` (default)
- `outbox.cleanup.enabled=true` (default)
- property `cron.delete.old.outbox.messages` is set

Cleanup deletes rows with `created_at < now() - retention`.

## Properties

```yaml
outbox:
  enabled: true

outbox:
  cleanup:
    enabled: true
    retention: "P7D"          # ISO-8601 duration, e.g. P1D, PT24H
    lock-at-most-for: "PT5M"
    lock-at-least-for: "PT10S"

cron:
  delete:
    old:
      outbox: "0 0 0/1 * * *" # Check to delete every 1 hour

scheduler:
  lock-at-most-for: "PT10M"
```

> This starter **does not** include a Kafka publisher. It only enqueues messages in the outbox table.
