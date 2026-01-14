package com.wordpress.kkaravitis.banking.antifraud.domain;

import com.wordpress.kkaravitis.banking.antifraud.domain.BlacklistedAccountEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistedAccountRepository extends JpaRepository<BlacklistedAccountEntity, String> {
    List<BlacklistedAccountEntity> findByAccountIdIn(Collection<String> accountIds);
}
