package com.example.account.repository;

import com.example.account.entity.DailyTransactionSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Optional;

public interface DailyTransactionSummaryRepository extends JpaRepository<DailyTransactionSummary, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DailyTransactionSummary d WHERE d.accountId = :accountId AND d.date = :date")
    Optional<DailyTransactionSummary> findByAccountIdAndDateWithLock(Long accountId, Instant date);
}