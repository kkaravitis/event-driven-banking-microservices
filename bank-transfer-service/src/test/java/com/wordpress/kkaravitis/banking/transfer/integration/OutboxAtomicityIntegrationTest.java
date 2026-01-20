package com.wordpress.kkaravitis.banking.transfer.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.account.api.commands.ReserveFundsCommand;
import com.wordpress.kkaravitis.banking.antifraud.api.commands.CheckFraudCommand;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudApprovedEvent;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudEventType;
import com.wordpress.kkaravitis.banking.idempotency.inbox.InboxMessageRepository;
import com.wordpress.kkaravitis.banking.outbox.OutboxMessage;
import com.wordpress.kkaravitis.banking.outbox.OutboxMessageRepository;
import com.wordpress.kkaravitis.banking.transfer.TransferService;
import com.wordpress.kkaravitis.banking.transfer.TransferService.InitiateTransferCommand;
import com.wordpress.kkaravitis.banking.transfer.TransferService.SagaParticipantReply;
import com.wordpress.kkaravitis.banking.transfer.TransferServiceApplication;
import com.wordpress.kkaravitis.banking.transfer.adapter.outbound.SagaJpaRepository;
import com.wordpress.kkaravitis.banking.transfer.adapter.outbound.TransferJpaRepository;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEntity;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaData;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import com.wordpress.kkaravitis.banking.transfer.domain.TransferState;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Outbox atomicity integration tests.
 *
 * <p>These tests prove the key property for Episode 1:
 * when Transfer/Saga state is persisted, the outbox row that represents the next command
 * is persisted in the <b>same</b> database transaction.
 */
@Testcontainers
@SpringBootTest(
      classes = TransferServiceApplication.class,
      properties = {
            "spring.kafka.listener.auto-startup=false",
            "spring.task.scheduling.enabled=false"
      }
)
class OutboxAtomicityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("transfer_test")
          .withUsername("test")
          .withPassword("test");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Kafka listeners are disabled, but Boot still wants a value.
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Autowired private TransferService transferService;
    @Autowired private TransferJpaRepository transferJpaRepository;
    @Autowired private SagaJpaRepository sagaJpaRepository;
    @Autowired private OutboxMessageRepository outboxMessageRepository;
    @Autowired private InboxMessageRepository inboxMessageRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void setup() {
        txTemplate = new TransactionTemplate(transactionManager);
        jdbcTemplate.execute("TRUNCATE TABLE outbox_message, inbox_message, saga, transfer CASCADE");
    }

    @Test
    void startTransfer_persistsTransferSagaAndOutboxAtomically() {
        InitiateTransferCommand cmd = InitiateTransferCommand.builder()
              .customerId("customer-1")
              .fromAccountId("A")
              .toAccountId("B")
              .amount(new BigDecimal("10.00"))
              .currency("EUR")
              .build();

        // 1) Atomicity proof via rollback: nothing remains if the transaction rolls back.
        txTemplate.execute(status -> {
            DomainResult res = transferService.startTransfer(cmd);
            assertTrue(res.isValid());
            status.setRollbackOnly();
            return null;
        });

        assertEquals(0L, transferJpaRepository.count(), "Transfer must not be persisted on rollback");
        assertEquals(0L, sagaJpaRepository.count(), "Saga must not be persisted on rollback");
        assertEquals(0L, outboxMessageRepository.count(), "Outbox must not be persisted on rollback");

        // 2) Commit path: all 3 records exist together.
        DomainResult committed = transferService.startTransfer(cmd);
        assertTrue(committed.isValid());
        assertNotNull(committed.getTransferId());

        UUID transferId = committed.getTransferId();

        Transfer transfer = transferJpaRepository.findById(transferId).orElseThrow();
        assertEquals(TransferState.REQUESTED, transfer.getState());

        List<SagaEntity> sagas = sagaJpaRepository.findAll();
        assertEquals(1, sagas.size(), "Expected exactly one execution saga");
        SagaEntity saga = sagas.get(0);
        TransferExecutionSagaData sagaData = readSagaData(saga.getSagaDataJson());
        assertEquals(transferId, sagaData.getTransferId());
        assertEquals(TransferExecutionSagaStatus.FRAUD_CHECK_PENDING, sagaData.getStatus());

        List<OutboxMessage> outbox = outboxMessageRepository.findAll();
        assertEquals(1, outbox.size(), "Expected exactly one outbox row (CheckFraudCommand)");
        OutboxMessage outboxMessage = outbox.get(0);
        assertEquals(saga.getSagaId(), outboxMessage.getCorrelationId(), "Outbox correlation_id must be the saga id");
        assertEquals(CheckFraudCommand.MESSAGE_TYPE, outboxMessage.getMessageType());

        CheckFraudCommand payload = readOutboxPayload(outboxMessage.getPayload(), CheckFraudCommand.class);
        assertEquals(transferId, payload.transferId());
        assertEquals("customer-1", payload.customerId());
        assertEquals("A", payload.fromAccountId());
        assertEquals("B", payload.toAccountId());
        assertEquals(new BigDecimal("10.00"), payload.amount());
        assertEquals("EUR", payload.currency());
    }

    @Test
    void oneReply_advancesSagaAndWritesOutboxAtomically() {
        // Start transfer (committed): creates Transfer + Saga + outbox(CheckFraud)
        DomainResult startResult = transferService.startTransfer(
              InitiateTransferCommand.builder()
                    .customerId("customer-1")
                    .fromAccountId("A")
                    .toAccountId("B")
                    .amount(new BigDecimal("10.00"))
                    .currency("EUR")
                    .build());

        assertTrue(startResult.isValid());
        UUID transferId = startResult.getTransferId();
        UUID sagaId = getSingleSagaId();

        assertEquals(1L, outboxMessageRepository.count(), "Start should create 1 outbox row");
        assertEquals(0L, inboxMessageRepository.count(), "No inbox rows before replies");

        SagaParticipantReply reply = new SagaParticipantReply(
              "msg-atomic-1",
              sagaId.toString(),
              FraudEventType.FRAUD_APPROVED.getMessageType(),
              toJson(new FraudApprovedEvent(transferId))
        );

        // 1) Atomicity proof via rollback: saga state and outbox must not advance/appear.
        txTemplate.execute(status -> {
            transferService.handleTransferExecutionParticipantReply(reply);
            status.setRollbackOnly();
            return null;
        });

        assertEquals(1L, outboxMessageRepository.count(), "No new outbox rows on rollback");
        assertEquals(0L, inboxMessageRepository.count(), "Inbox insert must roll back too");

        SagaEntity sagaAfterRollback = sagaJpaRepository.findById(sagaId).orElseThrow();
        assertEquals(TransferExecutionSagaStatus.FRAUD_CHECK_PENDING.name(), sagaAfterRollback.getSagaState(),
              "Saga must not advance on rollback");

        Transfer transferAfterRollback = transferJpaRepository.findById(transferId).orElseThrow();
        assertEquals(TransferState.REQUESTED, transferAfterRollback.getState(), "Transfer state unchanged");

        // 2) Commit path: now advances the saga and emits the next command.
        SagaParticipantReply commitReply = new SagaParticipantReply(
              "msg-atomic-commit",
              sagaId.toString(),
              FraudEventType.FRAUD_APPROVED.getMessageType(),
              toJson(new FraudApprovedEvent(transferId))
        );
        transferService.handleTransferExecutionParticipantReply(commitReply);

        assertEquals(2L, outboxMessageRepository.count(), "A single saga step should add exactly one outbox row");
        assertEquals(1L, inboxMessageRepository.count(), "Inbox should store the message_id");

        SagaEntity sagaAfterCommit = sagaJpaRepository.findById(sagaId).orElseThrow();
        assertEquals(TransferExecutionSagaStatus.FUNDS_RESERVATION_PENDING.name(), sagaAfterCommit.getSagaState(),
              "FraudApproved should advance saga to FUNDS_RESERVATION_PENDING");

        Long reserveFundsCount = jdbcTemplate.queryForObject(
              "select count(*) from outbox_message where correlation_id = ? and message_type = ?",
              Long.class,
              sagaId,
              ReserveFundsCommand.MESSAGE_TYPE
        );
        assertEquals(1L, reserveFundsCount, "FraudApproved should enqueue ReserveFundsCommand exactly once");
    }

    private UUID getSingleSagaId() {
        List<SagaEntity> sagas = sagaJpaRepository.findAll();
        assertEquals(1, sagas.size(), "Expected exactly one saga row");
        return sagas.get(0).getSagaId();
    }

    private TransferExecutionSagaData readSagaData(String json) {
        try {
            return objectMapper.readValue(json, TransferExecutionSagaData.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T readOutboxPayload(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
