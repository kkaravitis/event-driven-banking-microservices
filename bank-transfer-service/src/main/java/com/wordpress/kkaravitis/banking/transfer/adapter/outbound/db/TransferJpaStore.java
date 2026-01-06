package com.wordpress.kkaravitis.banking.transfer.adapter.outbound.db;

import com.wordpress.kkaravitis.banking.transfer.adapter.outbound.db.TransferJpaRepository;
import com.wordpress.kkaravitis.banking.transfer.application.ports.TransferStore;
import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class TransferJpaStore implements TransferStore {
    private final TransferJpaRepository transferRepository;

    @Override
    public Optional<Transfer> load(UUID transferId) {
        return transferRepository.findById(transferId);
    }

    @Override
    public void save(Transfer transfer) {
        transferRepository.save(transfer);
    }
}
