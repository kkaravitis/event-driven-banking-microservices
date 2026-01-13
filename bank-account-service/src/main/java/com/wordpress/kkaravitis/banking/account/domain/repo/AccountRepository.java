package com.wordpress.kkaravitis.banking.account.domain.repo;

import com.wordpress.kkaravitis.banking.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {

}
