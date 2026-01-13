package com.wordpress.kkaravitis.banking.transfer.application.ports;

import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import java.util.Optional;
import java.util.UUID;

public interface TransferStore extends AggregateStore<Transfer> {
}
