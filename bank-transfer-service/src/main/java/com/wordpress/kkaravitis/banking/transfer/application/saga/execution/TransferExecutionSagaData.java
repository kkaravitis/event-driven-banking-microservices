package com.wordpress.kkaravitis.banking.transfer.application.saga.execution;

import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaData;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FraudApprovedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FraudRejectedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FundsReservationFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FundsReservationFailedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FundsReservedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;



@Builder(toBuilder = true)
@Getter
public class TransferExecutionSagaData implements SagaData<TransferExecutionSagaStatus> {

    private final UUID sagaId;
    private final UUID transferId;
    private final String fromAccountId;
    private final String toAccountId;
    private final BigDecimal amount;
    private final String currency;

    private final TransferExecutionSagaStatus status;
    private final String fundsReservationId;
    private final String customerId;


    @Override
    public SagaData<TransferExecutionSagaStatus> withStatus(TransferExecutionSagaStatus newStatus) {
        return toBuilder().status(newStatus).build();
    }

    public TransferExecutionSagaData withReservationId(String reservationId) {
        return toBuilder().fundsReservationId(reservationId).build();
    }
}
