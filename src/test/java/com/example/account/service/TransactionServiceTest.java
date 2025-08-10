package com.example.account.service;

import com.example.account.dto.request.TransactionRequest;
import com.example.account.dto.request.TransferRequest;
import com.example.account.entity.Account;
import com.example.account.entity.DailyTransactionSummary;
import com.example.account.entity.Transaction;
import com.example.account.entity.type.AccountStatus;
import com.example.account.entity.type.TransactionStatus;
import com.example.account.entity.type.TransactionType;
import com.example.account.exception.AccountNotActiveException;
import com.example.account.exception.AccountNotFoundException;
import com.example.account.exception.DailyLimitExceededException;
import com.example.account.exception.InsufficientBalanceException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.DailyTransactionSummaryRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.util.LockUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DailyTransactionSummaryRepository dailyTransactionSummaryRepository;

    @Mock
    private LockUtil lockUtil;

    @Mock
    private RLock lock;  // RLock 추가

    @Mock
    private RedissonClient redissonClient;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Redis Lock 모킹 설정
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
    }


    @Test
    void transfer_Success() {
        // given
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("1234567890")
                .toAccountNumber("0987654321")
                .amount(BigDecimal.valueOf(1000))
                .build();

        Account fromAccount = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .balance(BigDecimal.valueOf(2000))
                .status(AccountStatus.ACTIVE)
                .dailyTransferLimit(BigDecimal.valueOf(10000))
                .build();

        Account toAccount = Account.builder()
                .id(2L)
                .accountNumber("0987654321")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        DailyTransactionSummary summary = DailyTransactionSummary.builder()
                .accountId(1L)
                .totalTransfer(BigDecimal.ZERO)
                .date(Instant.now())
                .build();

        Transaction mockTransaction = Transaction.builder()
                .id(1L)
                .transactionId("TX123")
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(1000))
                .type(TransactionType.TRANSFER)  // 필수 필드 추가
                .status(TransactionStatus.COMPLETED)  // 필수 필드 추가
                .createdAt(Instant.now())
                .build();

        given(accountRepository.findByAccountNumberWithLock(request.getFromAccountNumber()))
                .willReturn(Optional.of(fromAccount));
        given(accountRepository.findByAccountNumberWithLock(request.getToAccountNumber()))
                .willReturn(Optional.of(toAccount));
        given(dailyTransactionSummaryRepository.findByAccountIdAndDateWithLock(eq(1L), any()))
                .willReturn(Optional.of(summary));
        given(transactionRepository.save(any(Transaction.class)))
                .willReturn(mockTransaction);  // 수정된 부분

        // when
        var response = transactionService.transfer(request);

        // then
        assertThat(response).isNotNull();
        verify(transactionRepository).save(any(Transaction.class));
        verify(dailyTransactionSummaryRepository).save(any(DailyTransactionSummary.class));
    }

    @Test
    void transfer_DailyLimitExceeded() {
        // given
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("1234567890")
                .toAccountNumber("0987654321")
                .amount(BigDecimal.valueOf(2000))
                .build();

        Account fromAccount = Account.builder()
                .id(1L)  // ID 추가
                .accountNumber("1234567890")
                .balance(BigDecimal.valueOf(3000))
                .status(AccountStatus.ACTIVE)
                .dailyTransferLimit(BigDecimal.valueOf(1000))
                .build();

        Account toAccount = Account.builder()
                .id(2L)  // ID 추가
                .accountNumber("0987654321")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        DailyTransactionSummary summary = DailyTransactionSummary.builder()
                .id(1L)
                .accountId(1L)
                .totalTransfer(BigDecimal.ZERO)
                .totalWithdraw(BigDecimal.ZERO)
                .date(Instant.now())
                .build();

        // 올바른 메소드로 모킹 변경
        given(accountRepository.findByAccountNumberWithLock(request.getFromAccountNumber()))
                .willReturn(Optional.of(fromAccount));
        given(accountRepository.findByAccountNumberWithLock(request.getToAccountNumber()))
                .willReturn(Optional.of(toAccount));
        given(dailyTransactionSummaryRepository.findByAccountIdAndDateWithLock(eq(1L), any()))
                .willReturn(Optional.of(summary));

        // when & then
        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(DailyLimitExceededException.class)
                .hasMessage("일일 이체 한도를 초과했습니다.");
    }

    @Test
    void transfer_AccountNotActive() {
        // given
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("1234567890")
                .toAccountNumber("0987654321")
                .amount(BigDecimal.valueOf(1000))
                .build();

        Account fromAccount = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .balance(BigDecimal.valueOf(2000))
                .status(AccountStatus.INACTIVE)
                .build();

        Account toAccount = Account.builder()
                .id(2L)
                .accountNumber("0987654321")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        // 올바른 메소드로 모킹 변경
        given(accountRepository.findByAccountNumberWithLock(request.getFromAccountNumber()))
                .willReturn(Optional.of(fromAccount));
        given(accountRepository.findByAccountNumberWithLock(request.getToAccountNumber()))
                .willReturn(Optional.of(toAccount));

        // Redis lock 관련 모킹은 @BeforeEach에서 이미 설정됨

        // when & then
        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void deposit_Success() {
        // given
        TransactionRequest request = TransactionRequest.builder()
                .accountNumber("1234567890")
                .amount(BigDecimal.valueOf(1000))
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .balance(BigDecimal.valueOf(2000))
                .status(AccountStatus.ACTIVE)
                .build();

        Transaction mockTransaction = Transaction.builder()
                .id(1L)
                .transactionId("TX123")
                .toAccount(account)
                .amount(BigDecimal.valueOf(1000))
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        given(accountRepository.findByAccountNumberWithLock(request.getAccountNumber()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any(Transaction.class)))
                .willReturn(mockTransaction);

        // when
        var response = transactionService.deposit(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo("TX123");
        assertThat(response.getAmount()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(response.getType()).isEqualTo(TransactionType.DEPOSIT.name());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void deposit_AccountNotActive() {
        // given
        TransactionRequest request = TransactionRequest.builder()
                .accountNumber("1234567890")
                .amount(BigDecimal.valueOf(1000))
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .balance(BigDecimal.valueOf(2000))
                .status(AccountStatus.INACTIVE)
                .build();

        given(accountRepository.findByAccountNumberWithLock(request.getAccountNumber()))
                .willReturn(Optional.of(account));

        // when & then
        assertThatThrownBy(() -> transactionService.deposit(request))
                .isInstanceOf(AccountNotActiveException.class)
                .hasMessage("활성화 된 계좌가 아닙니다.");
    }

    @Test
    void withdraw_Success() {
        // given
        TransactionRequest request = TransactionRequest.builder()
                .accountNumber("1234567890")
                .amount(BigDecimal.valueOf(1000))
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .balance(BigDecimal.valueOf(2000))
                .status(AccountStatus.ACTIVE)
                .dailyWithdrawLimit(BigDecimal.valueOf(5000))
                .build();

        DailyTransactionSummary summary = DailyTransactionSummary.builder()
                .id(1L)
                .accountId(1L)
                .totalWithdraw(BigDecimal.ZERO)
                .date(Instant.now())
                .build();

        Transaction mockTransaction = Transaction.builder()
                .id(1L)
                .transactionId("TX123")
                .fromAccount(account)
                .amount(BigDecimal.valueOf(1000))
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        given(accountRepository.findByAccountNumberWithLock(request.getAccountNumber()))
                .willReturn(Optional.of(account));
        given(dailyTransactionSummaryRepository.findByAccountIdAndDateWithLock(eq(1L), any()))
                .willReturn(Optional.of(summary));
        given(transactionRepository.save(any(Transaction.class)))
                .willReturn(mockTransaction);

        // when
        var response = transactionService.withdraw(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo("TX123");
        assertThat(response.getAmount()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(response.getType()).isEqualTo(TransactionType.WITHDRAW.name());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_InsufficientBalance() {
        // given
        TransactionRequest request = TransactionRequest.builder()
                .accountNumber("1234567890")
                .amount(BigDecimal.valueOf(2000))
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .balance(BigDecimal.valueOf(1000))
                .status(AccountStatus.ACTIVE)
                .dailyWithdrawLimit(BigDecimal.valueOf(5000))
                .build();

        given(accountRepository.findByAccountNumberWithLock(request.getAccountNumber()))
                .willReturn(Optional.of(account));

        // when & then
        assertThatThrownBy(() -> transactionService.withdraw(request))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("잔액이 부족합니다.");
    }

    @Test
    void withdraw_DailyLimitExceeded() {
        // given
        TransactionRequest request = TransactionRequest.builder()
                .accountNumber("1234567890")
                .amount(BigDecimal.valueOf(2000))
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .balance(BigDecimal.valueOf(3000))
                .status(AccountStatus.ACTIVE)
                .dailyWithdrawLimit(BigDecimal.valueOf(1000))
                .build();

        DailyTransactionSummary summary = DailyTransactionSummary.builder()
                .id(1L)
                .accountId(1L)
                .totalWithdraw(BigDecimal.ZERO)
                .date(Instant.now())
                .build();

        given(accountRepository.findByAccountNumberWithLock(request.getAccountNumber()))
                .willReturn(Optional.of(account));
        given(dailyTransactionSummaryRepository.findByAccountIdAndDateWithLock(eq(1L), any()))
                .willReturn(Optional.of(summary));

        // when & then
        assertThatThrownBy(() -> transactionService.withdraw(request))
                .isInstanceOf(DailyLimitExceededException.class)
                .hasMessage("일일 출금 한도를 초과했습니다.");
    }
}