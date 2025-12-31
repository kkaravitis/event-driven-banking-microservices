package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.steps;

import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaData;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaData;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaStepHandler;

public class FinalizationNextStepHandler implements TransferExecutionSagaStepHandler {

    @Override
    public TransferExecutionSagaStatus currentSagaStatus() {
        return TransferExecutionSagaStatus.FINALIZATION_PENDING;
    }

    @Override
    public SagaStepResult<TransferExecutionSagaStatus> handle(SagaEvent event, SagaData<TransferExecutionSagaStatus> sagaData) {
        TransferExecutionSagaData transferExecutionSagaData = (TransferExecutionSagaData) sagaData;

        return null;
    }
}
