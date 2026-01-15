package com.wordpress.kkaravitis.banking.account.api.commands;

import static com.wordpress.kkaravitis.banking.account.api.commands.AccountCommandType.RELEASE_FUNDS;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Getter
public class ReleaseFundsCommand {
    public static final String MESSAGE_TYPE = RELEASE_FUNDS.name();

    private UUID transferId;
    private String reservationId;
    private BigDecimal amount;
    private String fromAccountId;
    private String customerId;
}
