package com.wordpress.kkaravitis.banking.antifraud.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistedAccountRepository extends JpaRepository<BlacklistedAccountEntity, String> {
    List<BlacklistedAccountEntity> findByAccountIdIn(Collection<String> accountIds);
}
