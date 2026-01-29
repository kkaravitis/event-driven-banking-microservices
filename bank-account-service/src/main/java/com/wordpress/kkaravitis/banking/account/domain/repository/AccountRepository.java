package com.wordpress.kkaravitis.banking.account.domain.repository;

import com.wordpress.kkaravitis.banking.account.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {

}
