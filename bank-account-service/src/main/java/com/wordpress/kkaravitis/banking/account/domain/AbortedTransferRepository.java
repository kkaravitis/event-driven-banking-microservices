package com.wordpress.kkaravitis.banking.account.domain;

import com.wordpress.kkaravitis.banking.account.domain.AbortedTransfer;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbortedTransferRepository extends JpaRepository<AbortedTransfer, UUID> {
}
