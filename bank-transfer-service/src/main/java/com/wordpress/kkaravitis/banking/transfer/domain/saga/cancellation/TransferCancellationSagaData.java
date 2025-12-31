package com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation;

import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaData;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaStatus;
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
public class TransferCancellationSagaData implements SagaData<TransferCancellationSagaStatus> {
    private UUID sagaId;
    private UUID transferId;
    private TransferCancellationSagaStatus status;
}
