package com.wordpress.kkaravitis.banking.transfer.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransferTest {

    @Test
    @DisplayName("startCancellation: REQUESTED -> CANCEL_PENDING")
    void startCancellation_fromRequested_movesToCancelPending() {
        // given
        Transfer transfer = newTransfer();

        // when
        DomainResult result = transfer.startCancellation();

        // then
        assertTrue(result.isValid(), "Expected successful cancellation start");
        assertEquals(TransferState.CANCEL_PENDING, transfer.getState());
    }

    @Test
    @DisplayName("startCancellation: COMPLETION_PENDING -> CANCEL_TOO_LATE (no state change)")
    void startCancellation_fromCompletionPending_returnsCancelTooLate_andKeepsState() {
        Transfer transfer = newTransfer();
        transfer.startCompletion("res-1");
        assertEquals(TransferState.COMPLETION_PENDING, transfer.getState());

        DomainResult result = transfer.startCancellation();

        assertFalse(result.isValid(), "Expected cancellation to be rejected as too late");
        assertNotNull(result.getError());
        assertEquals(DomainErrorCode.CANCEL_TOO_LATE, result.getError().code());
        assertEquals(TransferState.COMPLETION_PENDING, transfer.getState());
    }

    @Test
    @DisplayName("startCompletion: REQUESTED -> COMPLETION_PENDING and sets reservationId")
    void startCompletion_fromRequested_movesToCompletionPending_andSetsReservationId() {
        Transfer transfer = newTransfer();

        DomainResult result = transfer.startCompletion("res-123");

        assertTrue(result.isValid(), "Expected successful completion start");
        assertEquals(TransferState.COMPLETION_PENDING, transfer.getState());
        assertEquals("res-123", transfer.getFundsReservationId());
    }

    @Test
    @DisplayName("startCompletion: CANCEL_PENDING -> COMPLETE_TOO_LATE (no state change)")
    void startCompletion_fromCancelPending_returnsCompleteTooLate_andKeepsState() {
        Transfer transfer = newTransfer();
        transfer.startCancellation();
        assertEquals(TransferState.CANCEL_PENDING, transfer.getState());

        DomainResult result = transfer.startCompletion("res-1");

        assertFalse(result.isValid(), "Expected completion to be rejected as too late");
        assertNotNull(result.getError());
        assertEquals(DomainErrorCode.COMPLETE_TOO_LATE, result.getError().code());
        assertEquals(TransferState.CANCEL_PENDING, transfer.getState());
        assertNull(transfer.getFundsReservationId(), "Reservation id must not be set when completion is rejected");
    }

    @Test
    @DisplayName("markCompleted: only allowed from COMPLETION_PENDING")
    void markCompleted_onlyFromCompletionPending() {
        // Disallowed from REQUESTED
        Transfer requested = newTransfer();
        DomainResult bad = requested.markCompleted();
        assertFalse(bad.isValid(), "Expected completion to be rejected from REQUESTED");
        assertNotNull(bad.getError());
        assertEquals(DomainErrorCode.UNEXPECTED_TRANSITION, bad.getError().code());
        assertEquals(TransferState.REQUESTED, requested.getState());

        // Allowed from COMPLETION_PENDING
        Transfer completionPending = newTransfer();
        completionPending.startCompletion("res-1");
        DomainResult ok = completionPending.markCompleted();
        assertTrue(ok.isValid(), "Expected completion to succeed from COMPLETION_PENDING");
        assertEquals(TransferState.COMPLETED, completionPending.getState());
    }

    @Test
    @DisplayName("markCancelled: only allowed from CANCEL_PENDING")
    void markCancelled_onlyFromCancelPending() {
        // Disallowed from REQUESTED
        Transfer requested = newTransfer();
        DomainResult bad = requested.markCancelled();
        assertFalse(bad.isValid(), "Expected cancellation finalization to be rejected from REQUESTED");
        assertNotNull(bad.getError());
        assertEquals(DomainErrorCode.UNEXPECTED_TRANSITION, bad.getError().code());
        assertEquals(TransferState.REQUESTED, requested.getState());

        // Allowed from CANCEL_PENDING
        Transfer cancelPending = newTransfer();
        cancelPending.startCancellation();
        DomainResult ok = cancelPending.markCancelled();
        assertTrue(ok.isValid(), "Expected cancellation finalization to succeed from CANCEL_PENDING");
        assertEquals(TransferState.CANCELLED, cancelPending.getState());
    }

    private static Transfer newTransfer() {
        return Transfer.createNew(
              UUID.randomUUID(),
              "acc-from",
              "acc-to",
              new BigDecimal("10.00"),
              "EUR"
        );
    }
}