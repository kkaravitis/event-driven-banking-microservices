package com.wordpress.kkaravitis.banking.account.domain.values;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DomainResult {
    private final DomainError error;       // null when valid

    public boolean isValid() {
        return error == null;
    }

    public static DomainResult ok() {
        return DomainResult.builder()
              .build();
    }

    public static DomainResult fail(DomainErrorCode code, String message) {
        return DomainResult.builder()
              .error(new DomainError(code, message))
              .build();
    }
}
