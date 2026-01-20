package com.wordpress.kkaravitis.banking.transfer.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudApprovedEvent;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudEventType;
import com.wordpress.kkaravitis.banking.idempotency.inbox.InboxMessageRepository;
import com.wordpress.kkaravitis.banking.outbox.OutboxMessageRepository;
import com.wordpress.kkaravitis.banking.transfer.TransferService;
import com.wordpress.kkaravitis.banking.transfer.TransferService.InitiateTransferCommand;
import com.wordpress.kkaravitis.banking.transfer.TransferService.SagaParticipantReply;
import com.wordpress.kkaravitis.banking.transfer.TransferServiceApplication;
import com.wordpress.kkaravitis.banking.transfer.adapter.outbound.SagaJpaRepository;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEntity;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaData;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
      classes = TransferServiceApplication.class,
      properties = {
            "spring.kafka.listener.auto-startup=false",
            "spring.task.scheduling.enabled=false"
      }
)
class InboxDedupeIntegrationTest {

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

        // We don't need Kafka for this test; prevent listeners from starting.
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Autowired private TransferService transferService;
    @Autowired private SagaJpaRepository sagaJpaRepository;
    @Autowired private OutboxMessageRepository outboxMessageRepository;
    @Autowired private InboxMessageRepository inboxMessageRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE outbox_message, inbox_message, saga, transfer CASCADE");
    }

    @Test
    void processingSameReplyTwiceWithSameMessageId_isDedupedByInbox() {
        DomainResult startResult = transferService.startTransfer(
              InitiateTransferCommand.builder()
                    .customerId("customer-1")
                    .fromAccountId("A")
                    .toAccountId("B")
                    .amount(new BigDecimal("10.00"))
                    .currency("EUR")
                    .build());

        assertTrue(startResult.isValid());
        assertNotNull(startResult.getTransferId());

        UUID transferId = startResult.getTransferId();
        UUID sagaId = findExecutionSagaIdForTransfer(transferId);

        long outboxBeforeReplies = outboxMessageRepository.count();

        SagaParticipantReply reply = new SagaParticipantReply(
              "msg-1",
              sagaId.toString(),
              FraudEventType.FRAUD_APPROVED.getMessageType(),
              toJson(new FraudApprovedEvent(transferId))
        );

        transferService.handleTransferExecutionParticipantReply(reply);
        transferService.handleTransferExecutionParticipantReply(reply);

        assertEquals(
              outboxBeforeReplies + 1,
              outboxMessageRepository.count(),
              "Duplicate reply should not create extra outbox rows"
        );

        assertEquals(
              1,
              inboxMessageRepository.count(),
              "Inbox should contain the message_id only once"
        );

        SagaEntity saga = sagaJpaRepository.findById(sagaId).orElseThrow();
        assertEquals(
              TransferExecutionSagaStatus.FUNDS_RESERVATION_PENDING.name(),
              saga.getSagaState(),
              "Saga should advance exactly one step"
        );
    }

    private UUID findExecutionSagaIdForTransfer(UUID transferId) {
        List<SagaEntity> sagas = sagaJpaRepository.findAll();
        assertEquals(1, sagas.size(), "Expected exactly one saga row after starting a transfer");

        SagaEntity saga = sagas.get(0);
        TransferExecutionSagaData data = readSagaData(saga.getSagaDataJson());
        assertEquals(transferId, data.getTransferId());

        return saga.getSagaId();
    }

    private TransferExecutionSagaData readSagaData(String json) {
        try {
            return objectMapper.readValue(json, TransferExecutionSagaData.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
