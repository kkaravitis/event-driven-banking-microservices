package com.wordpress.kkaravitis.banking.transfer.domain;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DomainResult {

    private final Transition transition;

    private final List<DomainError> errors;

    public boolean isValid() {
        return errors == null || errors.isEmpty();
    }

}
