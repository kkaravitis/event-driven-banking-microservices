package com.wordpress.kkaravitis.banking.transfer.application.ports;

import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEntity;
import java.util.Optional;
import java.util.UUID;

public interface SagaStore {
    Optional<SagaEntity> load(UUID sagaId);

    void save(SagaEntity sagaEntity);
}
