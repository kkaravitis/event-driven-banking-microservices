package com.wordpress.kkaravitis.banking.transfer.adapter.inbound;

import com.wordpress.kkaravitis.banking.transfer.TransferService;
import com.wordpress.kkaravitis.banking.transfer.TransferService.InitiateCancellationCommand;
import com.wordpress.kkaravitis.banking.transfer.TransferService.InitiateTransferCommand;
import com.wordpress.kkaravitis.banking.transfer.adapter.inbound.web.InitiateTransferDTO;
import com.wordpress.kkaravitis.banking.transfer.adapter.inbound.web.TransferResponse;
import com.wordpress.kkaravitis.banking.transfer.application.ports.TransferStore;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/banking/transfer")
public class TransferController {
    private static final String CUSTOMER_ID_HEADER = "X-CUSTOMER-ID";

    private final TransferService transferService;
    private final TransferStore transferStore;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransferResponse> initiate(@RequestHeader(CUSTOMER_ID_HEADER) String customerId,
          @RequestBody InitiateTransferDTO dto) {

        DomainResult domainResult = transferService.startTransfer(InitiateTransferCommand
              .builder()
                    .customerId(customerId)
                    .amount(dto.amount())
                    .currency(dto.currency())
                    .fromAccountId(dto.fromAccountId())
                    .toAccountId(dto.toAccountId())
              .build());

        if (domainResult.isValid()) {
            return ResponseEntity.accepted()
                  .body(new TransferResponse(domainResult.getTransferId().toString(),
                  null, null));
        }
        return ResponseEntity.badRequest().body(new TransferResponse(null,
              null,
              domainResult.getError().message()));

    }

    @PostMapping("/{transferId}/cancel")
    public ResponseEntity<TransferResponse> cancel(@RequestHeader(CUSTOMER_ID_HEADER) String customerId,
          @PathVariable String transferId) {
        DomainResult domainResult = transferService.startCancellation(InitiateCancellationCommand
              .builder()
                    .transferId(UUID.fromString(transferId))
                    .customerId(customerId)
              .build());
        if (domainResult.isValid()) {
            return ResponseEntity.accepted()
                  .body(new TransferResponse(domainResult.getTransferId().toString(),
                  null, null));
        }
        return ResponseEntity.badRequest().body(new TransferResponse(null,
              null,
              domainResult.getError().message()));
    }

    @GetMapping(value = "/{transferId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable String transferId) {
        Optional<Transfer> transfer = transferStore.load(UUID.fromString(transferId));
        return transfer.map(value -> ResponseEntity.ok(new TransferResponse(value.getId().toString(),
              value.getState().name(), null)))
              .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
