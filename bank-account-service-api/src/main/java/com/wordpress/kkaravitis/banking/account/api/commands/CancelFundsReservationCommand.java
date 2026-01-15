package com.wordpress.kkaravitis.banking.account.api.commands;

import static com.wordpress.kkaravitis.banking.account.api.commands.AccountCommandType.CANCEL_FUNDS_RESERVATION;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Getter
public class CancelFundsReservationCommand {
    public static final String MESSAGE_TYPE = CANCEL_FUNDS_RESERVATION.name();

    private UUID transferId;
    private String customerId;
}
