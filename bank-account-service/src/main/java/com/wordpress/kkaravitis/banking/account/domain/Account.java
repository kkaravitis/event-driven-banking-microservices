package com.wordpress.kkaravitis.banking.account.domain;

import com.wordpress.kkaravitis.banking.account.domain.values.DomainErrorCode;
import com.wordpress.kkaravitis.banking.account.domain.values.DomainResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = {"accountId"})
@Getter
@Entity
@Table(name = "account")
public class Account {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private String accountId;

    @Column(name = "available_balance", nullable = false)
    private BigDecimal availableBalance;

    @Column(name = "reserved_balance", nullable = false)
    private BigDecimal reservedBalance;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Version
    private long version;
    
    // -------- validation helpers (no mutation) --------

    public DomainResult<Void> validateReserve(BigDecimal amount, String currency) {
        DomainResult<Void> domainResult = validateAmountAndCurrency(amount, currency);
        if (!domainResult.isValid()) {
          return domainResult;
        }

        if (availableBalance.compareTo(amount) < 0) {
            return DomainResult.fail(
                  DomainErrorCode.INSUFFICIENT_AVAILABLE_FUNDS,
                  "Insufficient available funds on account %s".formatted(accountId)
            );
        }
        return DomainResult.ok();
    }

    public DomainResult<Void> validateReleaseReserved(BigDecimal amount, String currency) {
        DomainResult<Void> domainResult = validateAmountAndCurrency(amount, currency);
        if (!domainResult.isValid()) {
            return domainResult;
        }

        if (reservedBalance.compareTo(amount) < 0) {
            return DomainResult.fail(
                  DomainErrorCode.INSUFFICIENT_RESERVED_FUNDS,
                  "Insufficient reserved funds on account %s".formatted(accountId)
            );
        }
        return DomainResult.ok();
    }

    public DomainResult<Void> validateConsumeReserved(BigDecimal amount, String currency) {
        return validateReleaseReserved(amount, currency);
    }

    public DomainResult<Void> validateCredit(BigDecimal amount, String currency) {
        return validateAmountAndCurrency(amount, currency);
    }

    // -------- mutating operations --------

    public DomainResult<Void> reserve(BigDecimal amount, String currency) {
        DomainResult<Void> domainResult = validateReserve(amount, currency);
        if (!domainResult.isValid()) {
          return domainResult;
        }

        availableBalance = availableBalance.subtract(amount);
        reservedBalance = reservedBalance.add(amount);
        return DomainResult.ok();
    }

    public DomainResult<Void> releaseReserved(BigDecimal amount, String currency) {
        DomainResult<Void> domainResult = validateReleaseReserved(amount, currency);
        if (!domainResult.isValid()) {
            return domainResult;
        }

        reservedBalance = reservedBalance.subtract(amount);
        availableBalance = availableBalance.add(amount);
        return DomainResult.ok();
    }

    public DomainResult<Void> consumeReserved(BigDecimal amount, String currency) {
        DomainResult<Void> domainResult = validateConsumeReserved(amount, currency);
        if (!domainResult.isValid()) {
            return domainResult;
        }

        reservedBalance = reservedBalance.subtract(amount);
        return DomainResult.ok();
    }

    public DomainResult<Void> credit(BigDecimal amount, String currency) {
        DomainResult<Void> domainResult = validateCredit(amount, currency);
        if (!domainResult.isValid()) {
            return domainResult;
        }

        availableBalance = availableBalance.add(amount);
        return DomainResult.ok();
    }

    private DomainResult<Void> validateAmountAndCurrency(BigDecimal amount, String currency) {
        if (amount == null || amount.signum() <= 0) {
            return DomainResult.fail(DomainErrorCode.INVALID_AMOUNT, "Amount must be > 0");
        }
        if (currency == null || !currency.equals(this.currency)) {
            return DomainResult.fail(
                  DomainErrorCode.CURRENCY_MISMATCH,
                  "Currency mismatch for account %s (expected %s, got %s)".formatted(accountId, this.currency, currency)
            );
        }
        return DomainResult.ok();
    }
}
