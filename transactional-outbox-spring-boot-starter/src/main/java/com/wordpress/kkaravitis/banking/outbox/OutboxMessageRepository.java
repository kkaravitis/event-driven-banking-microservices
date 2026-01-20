package com.wordpress.kkaravitis.banking.outbox;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {

    @Modifying
    @Transactional
    @Query("""
        delete from OutboxMessage o
        where o.createdAt < :threshold
    """)
    int deleteOlderThan(@Param("threshold") Instant threshold);
}
