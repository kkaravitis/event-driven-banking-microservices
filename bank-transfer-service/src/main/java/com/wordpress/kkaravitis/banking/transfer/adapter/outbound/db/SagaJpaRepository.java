package com.wordpress.kkaravitis.banking.transfer.adapter.outbound.db;

import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaJpaRepository extends JpaRepository<SagaEntity, UUID> {
}
