package com.wordpress.kkaravitis.banking.account.domain.values;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DomainResult<T> {

    private final Transition transition;   // optional
    private final DomainError error;       // null when valid
    private final T value;                // outcome payload (null allowed)

    public boolean isValid() {
        return error == null;
    }

    public static <T> DomainResult<T> ok(T value, Transition transition) {
        return DomainResult.<T>builder()
              .value(value)
              .transition(transition)
              .build();
    }

    public static <T> DomainResult<T> ok(T value) {
        return ok(value, null);
    }

    public static <T> DomainResult<T> ok() {
        return ok(null, null);
    }

    public static <T> DomainResult<T> fail(DomainErrorCode code, String message) {
        return DomainResult.<T>builder()
              .error(new DomainError(code, message))
              .build();
    }
}
