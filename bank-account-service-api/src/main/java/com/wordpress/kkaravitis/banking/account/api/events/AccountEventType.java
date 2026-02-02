package com.wordpress.kkaravitis.banking.account.api.events;

import com.wordpress.kkaravitis.banking.account.api.events.incident.AccountServiceIncidentEvent;
import com.wordpress.kkaravitis.banking.common.BankingEventType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor()
public enum AccountEventType implements BankingEventType {
    FUNDS_RELEASED(FundsReleasedEvent.class),
    FUNDS_RELEASE_FAILED_DUE_TO_CANCEL(FundsReleaseFailedDueToCancelEvent.class),
    FUNDS_RESERVATION_CANCELLED(FundsReservationCancelledEvent.class),
    FUNDS_RESERVATION_FAILED_DUE_TO_CANCEL(FundsReservationFailedDueToCancelEvent.class),
    FUNDS_RESERVATION_FAILED(FundsReservationFailedEvent.class),
    FUNDS_RESERVED(FundsReservedEvent.class),
    TRANSFER_APPROVAL_FAILED_DUE_TO_CANCEL(TransferApprovalFailedDueToCancelEvent.class),
    TRANSFER_APPROVAL_FAILED(TransferApprovalFailedEvent.class),
    TRANSFER_FINALIZED(TransferFinalizedEvent.class),
    INCIDENT_EVENT(AccountServiceIncidentEvent.class);


    private final Class<?> payloadType;

    @Override
    public String getMessageType() {
        return this.name();
    }

    @Override
    public Class<?> getPayloadType() {
        return payloadType;
    }
}
