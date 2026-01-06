package com.wordpress.kkaravitis.banking.transfer.application.ports;

import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import java.util.Optional;
import java.util.UUID;

public interface TransferStore {

    Optional<Transfer> load(UUID transferId);

    void save(Transfer transfer);

}
