package com.wordpress.kkaravitis.banking.account.api.commands;

import static com.wordpress.kkaravitis.banking.account.api.commands.AccountCommandType.RESERVE_FUNDS;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReserveFundsCommand {
    public static final String MESSAGE_TYPE = RESERVE_FUNDS.name();

    private UUID transferId;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    private String customerId;
}
