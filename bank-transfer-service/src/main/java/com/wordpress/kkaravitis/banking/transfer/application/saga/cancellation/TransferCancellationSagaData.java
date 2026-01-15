package com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation;

import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaData;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder(toBuilder = true)
@Getter
public class TransferCancellationSagaData implements SagaData<TransferCancellationSagaStatus> {
    private UUID sagaId;
    private UUID transferId;
    private TransferCancellationSagaStatus status;
    private final String customerId;

    @Override
    public SagaData<TransferCancellationSagaStatus> withStatus(TransferCancellationSagaStatus newStatus) {
        return toBuilder().status(newStatus).build();
    }
}
