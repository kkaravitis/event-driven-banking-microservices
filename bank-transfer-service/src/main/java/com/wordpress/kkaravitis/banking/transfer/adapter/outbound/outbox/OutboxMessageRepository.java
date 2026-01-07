package com.wordpress.kkaravitis.banking.transfer.adapter.outbound.outbox;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    @Modifying
    @Transactional
    @Query("""
        delete from OutboxMessage o
        and o.createdAt < :threshold
    """)
    int deleteMessagesOlderThan(@Param("threshold") Instant threshold);
}
