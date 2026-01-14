package com.wordpress.kkaravitis.banking.transfer.domain;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DomainResult {

    private final UUID aggregateId;

    private final Transition transition;

    private final DomainError error;

    public boolean isValid() {
        return error == null;
    }

}
