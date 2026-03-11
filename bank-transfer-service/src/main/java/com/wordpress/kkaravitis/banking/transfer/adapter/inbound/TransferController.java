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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/banking/transfer")
public class TransferController {
    private final TransferService transferService;
    private final TransferStore transferStore;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransferResponse> initiate(
          @AuthenticationPrincipal Jwt jwt,
          @RequestBody InitiateTransferDTO dto) {

        String customerId = requireCustomerId(jwt);

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
    public ResponseEntity<TransferResponse> cancel(@AuthenticationPrincipal Jwt jwt,
          @PathVariable String transferId) {

        String customerId = requireCustomerId(jwt);

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
    public ResponseEntity<TransferResponse> getTransfer(@AuthenticationPrincipal Jwt jwt,
          @PathVariable String transferId) {

        String customerId = requireCustomerId(jwt);
        Optional<Transfer> optional = transferStore.load(UUID.fromString(transferId));

        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Transfer transfer = optional.get();

        if (!transfer.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Permission denied");
        }

        return ResponseEntity.ok(new TransferResponse(transfer.getId().toString(),
              transfer.getState().name(), null));
    }

    private static String requireCustomerId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing JWT principal");
        }
        String customerId = jwt.getClaimAsString("customerId");
        if (customerId == null || customerId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing customerId claim");
        }
        return customerId;
    }

}
