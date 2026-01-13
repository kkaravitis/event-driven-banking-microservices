package com.wordpress.kkaravitis.banking.transfer.application.ports;

import java.util.Optional;
import java.util.UUID;

public interface AggregateStore<T> {

    Optional<T> load(UUID aggregateId);

    void save(T aggregate);

}
