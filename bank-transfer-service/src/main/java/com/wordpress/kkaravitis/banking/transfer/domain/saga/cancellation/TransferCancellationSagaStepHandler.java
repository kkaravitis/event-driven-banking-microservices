package com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation;

import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaStepHandler;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaStatus;

public interface TransferCancellationSagaStepHandler extends SagaStepHandler<TransferCancellationSagaStatus> {

}
