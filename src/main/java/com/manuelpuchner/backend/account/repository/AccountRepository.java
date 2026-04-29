package com.manuelpuchner.backend.account.repository;

import com.manuelpuchner.backend.account.entity.Account;
import com.manuelpuchner.backend.transaction.entity.TransactionSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findBySourceAndOwnAccountIban(TransactionSource source, String ownAccountIban);

    List<Account> findAllBySource(TransactionSource source);
}
