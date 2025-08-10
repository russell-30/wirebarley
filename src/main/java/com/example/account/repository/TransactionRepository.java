package com.example.account.repository;

import com.example.account.entity.Account;
import com.example.account.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByFromAccountOrToAccount(
            Account fromAccount,
            Account toAccount,
            Pageable pageable
    );
}