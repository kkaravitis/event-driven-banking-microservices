package com.wordpress.kkaravitis.banking.account.domain.repository;

import com.wordpress.kkaravitis.banking.account.domain.entity.FundsReservation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundsReservationRepository extends JpaRepository<FundsReservation, String> {
    Optional<FundsReservation> findByTransferId(UUID transferId);
}
