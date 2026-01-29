package com.wordpress.kkaravitis.banking.account.domain.value;

import com.wordpress.kkaravitis.banking.account.domain.type.DomainErrorCode;

public record DomainError(DomainErrorCode code, String message) { }
