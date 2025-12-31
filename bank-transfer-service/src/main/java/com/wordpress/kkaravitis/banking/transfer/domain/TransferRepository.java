package com.wordpress.kkaravitis.banking.transfer.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

}
