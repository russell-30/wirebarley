package com.example.account.service;

import com.example.account.dto.request.AccountCreateRequest;
import com.example.account.entity.Account;
import com.example.account.entity.Transaction;
import com.example.account.entity.type.AccountStatus;
import com.example.account.entity.type.TransactionStatus;
import com.example.account.entity.type.TransactionType;
import com.example.account.exception.AccountNotFoundException;
import com.example.account.exception.DuplicateAccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_Success() {
        // given
        AccountCreateRequest request = new AccountCreateRequest();
        request.setAccountNumber("1234567890");

        Account account = Account.builder()
                .accountNumber("1234567890")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        given(accountRepository.existsByAccountNumber(request.getAccountNumber()))
                .willReturn(false);
        given(accountRepository.save(any(Account.class)))
                .willReturn(account);

        // when
        var response = accountService.createAccount(request);

        // then
        assertThat(response.getAccountNumber()).isEqualTo("1234567890");
        assertThat(response.getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.getStatus()).isEqualTo(AccountStatus.ACTIVE.name());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_DuplicateAccount() {
        // given
        AccountCreateRequest request = new AccountCreateRequest();
        request.setAccountNumber("1234567890");

        given(accountRepository.existsByAccountNumber(request.getAccountNumber()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(DuplicateAccountException.class)
                .hasMessage("이미 존재하는 계좌번호입니다.");
    }

    @Test
    void getAccount_Success() {
        // given
        String accountNumber = "1234567890";
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .balance(BigDecimal.valueOf(1000))
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        // when
        var response = accountService.getAccount(accountNumber);

        // then
        assertThat(response.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(response.getBalance()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(response.getStatus()).isEqualTo(AccountStatus.ACTIVE.name());
    }

    @Test
    void getAccount_NotFound() {
        // given
        String accountNumber = "1234567890";
        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountService.getAccount(accountNumber))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void getTransactionHistory_Success() {
        // given
        String accountNumber = "1234567890";
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId("TX123")
                .fromAccount(account)
                .amount(BigDecimal.valueOf(1000))
                .type(TransactionType.WITHDRAW)  // 트랜잭션 타입 추가
                .status(TransactionStatus.COMPLETED)  // 트랜잭션 상태 추가
                .createdAt(Instant.now())
                .build();

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        given(transactionRepository.findByFromAccountOrToAccount(
                any(Account.class),
                any(Account.class),
                any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(transaction)));

        // when
        var response = accountService.getTransactionHistory(accountNumber, 0, 20);

        // then
        assertThat(response.getTransactions()).hasSize(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getTransactions().get(0).getType())
                .isEqualTo(TransactionType.WITHDRAW.name());
    }
}