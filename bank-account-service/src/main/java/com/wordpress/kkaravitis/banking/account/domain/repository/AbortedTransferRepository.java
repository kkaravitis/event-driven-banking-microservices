package com.wordpress.kkaravitis.banking.account.domain.repository;

import com.wordpress.kkaravitis.banking.account.domain.entity.AbortedTransfer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbortedTransferRepository extends JpaRepository<AbortedTransfer, UUID> {

    boolean existsByTransferIdAndCustomerId(UUID transferId, String customerId);

    Optional<AbortedTransfer> findByTransferId(UUID transferId);

}
