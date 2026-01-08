package com.wordpress.kkaravitis.banking.transfer.adapter.outbound;

import com.wordpress.kkaravitis.banking.transfer.application.ports.SagaStore;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEntity;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class SagaJpaStore implements SagaStore {

    private final SagaJpaRepository sagaRepository;

    @Override
    public Optional<SagaEntity> load(UUID sagaId) {
        return sagaRepository.findById(sagaId);
    }

    @Override
    public void save(SagaEntity sagaEntity) {
        sagaRepository.save(sagaEntity);
    }
}
