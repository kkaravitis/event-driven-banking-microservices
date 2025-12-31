package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution;

import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaData;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class TransferExecutionSagaData implements SagaData<TransferExecutionSagaStatus> {

    private UUID sagaId;
    private UUID transferId;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;

    private TransferExecutionSagaStatus status;
    private String fraudDecision;
    private String fundsReservationId;
}
