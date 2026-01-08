package com.wordpress.kkaravitis.banking.transfer.adapter.outbound;

import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferJpaRepository extends JpaRepository<Transfer, UUID> {

}
