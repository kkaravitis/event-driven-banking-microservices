package com.wordpress.kkaravitis.banking.account.domain;

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

    public DomainResult validateReserve(BigDecimal amount, String currency) {
        DomainResult domainResult = validateAmountAndCurrency(amount, currency);
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

    public DomainResult validateReleaseReserved(BigDecimal amount, String currency) {
        DomainResult domainResult = validateAmountAndCurrency(amount, currency);
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

    public DomainResult validateConsumeReserved(BigDecimal amount, String currency) {
        return validateReleaseReserved(amount, currency);
    }

    public DomainResult validateCredit(BigDecimal amount, String currency) {
        return validateAmountAndCurrency(amount, currency);
    }

    // -------- mutating operations --------

    public DomainResult reserve(BigDecimal amount, String currency) {
        DomainResult domainResult = validateReserve(amount, currency);
        if (!domainResult.isValid()) {
          return domainResult;
        }

        availableBalance = availableBalance.subtract(amount);
        reservedBalance = reservedBalance.add(amount);
        return DomainResult.ok();
    }

    public DomainResult releaseReserved(BigDecimal amount, String currency) {
        DomainResult domainResult = validateReleaseReserved(amount, currency);
        if (!domainResult.isValid()) {
            return domainResult;
        }

        reservedBalance = reservedBalance.subtract(amount);
        availableBalance = availableBalance.add(amount);
        return DomainResult.ok();
    }

    public DomainResult consumeReserved(BigDecimal amount, String currency) {
        DomainResult domainResult = validateConsumeReserved(amount, currency);
        if (!domainResult.isValid()) {
            return domainResult;
        }

        reservedBalance = reservedBalance.subtract(amount);
        return DomainResult.ok();
    }

    public DomainResult credit(BigDecimal amount, String currency) {
        DomainResult domainResult = validateCredit(amount, currency);
        if (!domainResult.isValid()) {
            return domainResult;
        }

        availableBalance = availableBalance.add(amount);
        return DomainResult.ok();
    }

    private DomainResult validateAmountAndCurrency(BigDecimal amount, String currency) {
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
