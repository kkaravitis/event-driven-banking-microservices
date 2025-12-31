package com.wordpress.kkaravitis.banking.transfer.domain.saga;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaRepository extends JpaRepository<SagaEntity, UUID> {
}
