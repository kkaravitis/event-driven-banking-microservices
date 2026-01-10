package com.wordpress.kkaravitis.banking.account.api.commands;

import static com.wordpress.kkaravitis.banking.account.api.commands.AccountCommandType.FINALIZE_TRANSFER;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FinalizeTransferCommand {
    public static final String MESSAGE_TYPE = FINALIZE_TRANSFER.name();

    private UUID transferId;
    private String reservationId;
    private String customerId;
}
