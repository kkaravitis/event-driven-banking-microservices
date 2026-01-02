package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution;

import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaData;
import java.math.BigDecimal;
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
