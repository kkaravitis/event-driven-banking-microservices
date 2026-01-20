package com.wordpress.kkaravitis.banking.transfer.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.account.api.commands.FinalizeTransferCommand;
import com.wordpress.kkaravitis.banking.account.api.events.AccountEventType;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservedEvent;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudApprovedEvent;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudEventType;
import com.wordpress.kkaravitis.banking.outbox.OutboxMessageRepository;
import com.wordpress.kkaravitis.banking.transfer.TransferService;
import com.wordpress.kkaravitis.banking.transfer.TransferService.InitiateCancellationCommand;
import com.wordpress.kkaravitis.banking.transfer.TransferService.InitiateTransferCommand;
import com.wordpress.kkaravitis.banking.transfer.TransferService.SagaParticipantReply;
import com.wordpress.kkaravitis.banking.transfer.adapter.outbound.SagaJpaRepository;
import com.wordpress.kkaravitis.banking.transfer.adapter.outbound.TransferJpaRepository;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEntity;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import com.wordpress.kkaravitis.banking.transfer.domain.TransferState;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
      properties = {
            "spring.kafka.listener.auto-startup=false",
            "spring.task.scheduling.enabled=false"
      }
)
class ExecuteVsCancelRaceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("transfer_test")
          .withUsername("test")
          .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    TransferService transferService;

    @Autowired
    SagaJpaRepository sagaJpaRepository;

    @Autowired
    TransferJpaRepository transferJpaRepository;

    @Autowired
    OutboxMessageRepository outboxMessageRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void cleanDb() {
        // Order matters with FK constraints. CASCADE makes it deterministic.
        jdbcTemplate.execute("TRUNCATE TABLE inbox_message CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE outbox_message CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE saga CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE transfer CASCADE");
    }

    @Test
    void cancelWinsOverFundsReservedReply_noFinalizeCommandEmitted() throws Exception {
        // 1) Start execution saga
        var startResult = transferService.startTransfer(InitiateTransferCommand.builder()
              .customerId("customer-1")
              .fromAccountId("A")
              .toAccountId("B")
              .amount(new BigDecimal("10.00"))
              .currency("EUR")
              .build());

        assertTrue(startResult.isValid(), "startTransfer should be valid");
        UUID transferId = startResult.getTransferId();
        assertNotNull(transferId);

        assertEquals(1, sagaJpaRepository.count(), "Execution saga should be created");
        SagaEntity executionSaga = sagaJpaRepository.findAll().getFirst();
        UUID executionSagaId = executionSaga.getSagaId();

        transferService.handleTransferExecutionParticipantReply(new SagaParticipantReply(
              "msg-fraud-1",
              executionSagaId.toString(),
              FraudEventType.FRAUD_APPROVED.getMessageType(),
              objectMapper.writeValueAsString(new FraudApprovedEvent(transferId))
        ));

        SagaEntity afterFraud = sagaJpaRepository.findById(executionSagaId).orElseThrow();
        assertEquals(TransferExecutionSagaStatus.FUNDS_RESERVATION_PENDING.name(), afterFraud.getSagaState());

        // 3) Race: cancellation acquires CANCEL_PENDING first, then FundsReserved reply arrives
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch cancellationDone = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> cancelFuture = pool.submit(() -> {

                try {
                    startLatch.await(5, TimeUnit.SECONDS);

                    var cancelResult = transferService.startCancellation(InitiateCancellationCommand.builder()
                          .customerId("customer-1")
                          .transferId(transferId)
                          .build());
                    assertTrue(cancelResult.isValid(), "startCancellation should acquire the semantic lock");

                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    cancellationDone.countDown();
                }
            });

            Future<?> fundsReservedFuture = pool.submit(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);

                    assertTrue(cancellationDone.await(10, TimeUnit.SECONDS), "Cancellation did not complete in time");

                    transferService.handleTransferExecutionParticipantReply(new SagaParticipantReply(
                          "msg-funds-1",
                          executionSagaId.toString(),
                          AccountEventType.FUNDS_RESERVED.getMessageType(),
                          objectMapper.writeValueAsString(new FundsReservedEvent(transferId, "res-999"))
                    ));

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            startLatch.countDown();

            cancelFuture.get(15, TimeUnit.SECONDS);
            fundsReservedFuture.get(15, TimeUnit.SECONDS);

        } finally {
            pool.shutdownNow();
        }

        // 4) Assert: Transfer is locked for cancellation, not moved to COMPLETION_PENDING
        Transfer transfer = transferJpaRepository.findById(transferId).orElseThrow();
        assertEquals(TransferState.CANCEL_PENDING, transfer.getState(), "Cancellation should own the outcome");

        // 5) Assert: execution saga ends as CANCELLED_BY_CANCEL_SAGA
        SagaEntity afterFundsReserved = sagaJpaRepository.findById(executionSagaId).orElseThrow();
        assertEquals(TransferExecutionSagaStatus.CANCELLED_BY_CANCEL_SAGA.name(), afterFundsReserved.getSagaState());

        // 6) Assert: no FinalizeTransferCommand was emitted for the execution saga
        Integer finalizeCount = jdbcTemplate.queryForObject(
              "select count(*) from outbox_message where correlation_id = ? and message_type = ?",
              Integer.class,
              executionSagaId,
              FinalizeTransferCommand.MESSAGE_TYPE
        );
        assertEquals(0, finalizeCount);

        // Sanity: outbox still contains the earlier commands + cancel command (3 total)
        assertEquals(3, outboxMessageRepository.count());
    }
}
