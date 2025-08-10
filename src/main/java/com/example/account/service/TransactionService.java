package com.example.account.service;

import com.example.account.dto.request.TransactionRequest;
import com.example.account.dto.request.TransferRequest;
import com.example.account.dto.response.TransactionResponse;
import com.example.account.entity.*;
import com.example.account.entity.type.AccountStatus;
import com.example.account.entity.type.TransactionStatus;
import com.example.account.exception.AccountNotActiveException;
import com.example.account.exception.InsufficientBalanceException;
import com.example.account.repository.*;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.account.exception.DailyLimitExceededException;
import com.example.account.exception.AccountNotFoundException;
import com.example.account.entity.type.TransactionType;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 계좌 거래(입금, 출금, 이체)와 관련된 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final DailyTransactionSummaryRepository dailySummaryRepository;
    private final RedissonClient redissonClient;

    // 계좌 이체 수수료율 (1%)
    private static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.01");

    /**
     * 계좌에 입금을 처리합니다.
     *
     * @param request 입금 요청 정보 (계좌번호, 금액)
     * @return 처리된 거래 정보
     * @throws AccountNotFoundException 계좌를 찾을 수 없는 경우
     * @throws AccountNotActiveException 비활성화된 계좌인 경우
     */
    @Transactional
    public TransactionResponse deposit(TransactionRequest request) {
        Account account = getAccountWithLock(request.getAccountNumber());

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("활성화 된 계좌가 아닙니다.");
        }

        account.deposit(request.getAmount());

        Transaction transaction = createTransaction(null, account, request.getAmount(),
                TransactionType.DEPOSIT, null);

        return TransactionResponse.from(transaction);
    }

    /**
     * 계좌에서 출금을 처리합니다.
     *
     * @param request 출금 요청 정보 (계좌번호, 금액)
     * @return 처리된 거래 정보
     * @throws AccountNotFoundException 계좌를 찾을 수 없는 경우
     * @throws AccountNotActiveException 비활성화된 계좌인 경우
     * @throws InsufficientBalanceException 잔액이 부족한 경우
     * @throws DailyLimitExceededException 일일 출금 한도를 초과한 경우
     */
    @Transactional
    public TransactionResponse withdraw(TransactionRequest request) {
        Account account = getAccountWithLock(request.getAccountNumber());

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("활성화 된 계좌가 아닙니다.");
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("잔액이 부족합니다.");
        }

        checkDailyWithdrawLimit(account, request.getAmount());
        account.withdraw(request.getAmount());

        Transaction transaction = createTransaction(account, null, request.getAmount(),
                TransactionType.WITHDRAW, null);

        return TransactionResponse.from(transaction);
    }

    /**
     * 계좌 간 이체를 처리합니다.
     *
     * @param request 이체 요청 정보 (출금계좌, 입금계좌, 금액)
     * @return 처리된 거래 정보
     * @throws AccountNotFoundException 계좌를 찾을 수 없는 경우
     * @throws AccountNotActiveException 비활성화된 계좌인 경우
     * @throws InsufficientBalanceException 잔액이 부족한 경우
     * @throws DailyLimitExceededException 일일 이체 한도를 초과한 경우
     */
    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        Account fromAccount = getAccountWithLock(request.getFromAccountNumber());
        Account toAccount = getAccountWithLock(request.getToAccountNumber());

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("활성화 된 계좌가 아닙니다.");
        }

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("활성화 된 계좌가 아닙니다.");
        }

        BigDecimal fee = request.getAmount().multiply(TRANSFER_FEE_RATE);

        if (fromAccount.getBalance().add(fee).compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("잔액이 부족합니다.");
        }
        BigDecimal totalAmount = request.getAmount().add(fee);

        checkDailyTransferLimit(fromAccount, request.getAmount());
        fromAccount.withdraw(totalAmount);
        toAccount.deposit(request.getAmount());

        Transaction transaction = createTransaction(fromAccount, toAccount, request.getAmount(),
                TransactionType.TRANSFER, fee);

        return TransactionResponse.from(transaction);
    }

    /**
     * 계좌 정보를 조회하면서 동시에 분산 락을 획득합니다.
     *
     * @param accountNumber 조회할 계좌번호
     * @return 락이 걸린 계좌 정보
     * @throws AccountNotFoundException 계좌를 찾을 수 없는 경우
     * @throws RuntimeException 락 획득 실패 시
     */
    private Account getAccountWithLock(String accountNumber) {
        RLock lock = redissonClient.getLock("account:" + accountNumber);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("락 획득 실패");
            }
            return accountRepository.findByAccountNumberWithLock(accountNumber)
                    .orElseThrow(() -> new AccountNotFoundException("계좌를 찾을 수 없습니다."));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 일일 출금 한도를 확인합니다.
     *
     * @param account 확인할 계좌
     * @param amount 출금 금액
     * @throws DailyLimitExceededException 일일 출금 한도 초과 시
     */
    private void checkDailyWithdrawLimit(Account account, BigDecimal amount) {
        DailyTransactionSummary summary = getDailySummary(account.getId());
        if (summary.getTotalWithdraw().add(amount).compareTo(account.getDailyWithdrawLimit()) > 0) {
            throw new DailyLimitExceededException("일일 출금 한도를 초과했습니다.");
        }
        summary.addWithdraw(amount);
    }

    /**
     * 일일 이체 한도를 확인합니다.
     *
     * @param account 확인할 계좌
     * @param amount 이체 금액
     * @throws DailyLimitExceededException 일일 이체 한도 초과 시
     */
    private void checkDailyTransferLimit(Account account, BigDecimal amount) {
        DailyTransactionSummary summary = getDailySummary(account.getId());
        if (summary.getTotalTransfer().add(amount).compareTo(account.getDailyTransferLimit()) > 0) {
            throw new DailyLimitExceededException("일일 이체 한도를 초과했습니다.");
        }
        summary.addTransfer(amount);
        dailySummaryRepository.save(summary);
    }

    /**
     * 계좌의 일일 거래 요약 정보를 조회하거나 생성합니다.
     *
     * @param accountId 계좌 ID
     * @return 일일 거래 요약 정보
     */
    private DailyTransactionSummary getDailySummary(Long accountId) {
        return dailySummaryRepository.findByAccountIdAndDateWithLock(accountId, Instant.now())
                .orElseGet(() -> dailySummaryRepository.save(
                        DailyTransactionSummary.builder()
                                .accountId(accountId)
                                .date(Instant.now())
                                .totalWithdraw(BigDecimal.ZERO)
                                .totalTransfer(BigDecimal.ZERO)
                                .build()
                ));
    }

    /**
     * 새로운 거래 내역을 생성합니다.
     *
     * @param fromAccount 출금 계좌
     * @param toAccount 입금 계좌
     * @param amount 거래 금액
     * @param type 거래 유형
     * @param fee 수수료
     * @return 생성된 거래 내역
     */
    private Transaction createTransaction(Account fromAccount, Account toAccount, BigDecimal amount,
                                          TransactionType type, BigDecimal fee) {
        String transactionId = generateTransactionId();

        return transactionRepository.save(Transaction.builder()
                .fromAccount(fromAccount)
                .transactionId(transactionId)
                .toAccount(toAccount)
                .amount(amount)
                .type(type)
                .fee(fee)
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build());
    }

    /**
     * 고유한 거래 ID를 생성합니다.
     *
     * @return 생성된 거래 ID (TRX로 시작하는 8자리 문자열)
     */
    private String generateTransactionId() {
        return "TRX" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }


}