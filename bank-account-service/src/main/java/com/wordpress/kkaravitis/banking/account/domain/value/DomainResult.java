package com.wordpress.kkaravitis.banking.account.domain.value;

import com.wordpress.kkaravitis.banking.account.domain.type.DomainErrorCode;
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
