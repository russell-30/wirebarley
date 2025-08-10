package com.example.account.service;

import com.example.account.dto.request.AccountCreateRequest;
import com.example.account.dto.response.AccountResponse;
import com.example.account.dto.response.TransactionHistoryResponse;
import com.example.account.entity.Account;
import com.example.account.entity.Transaction;
import com.example.account.entity.type.AccountStatus;
import com.example.account.exception.DuplicateAccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.account.exception.AccountNotFoundException;

import java.math.BigDecimal;

/**
 * 계좌 관련 비즈니스 로직을 처리하는 서비스 클래스
 */
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * 새로운 계좌를 생성합니다.
     *
     * @param request 계좌 생성 요청 정보
     * @return 생성된 계좌 정보
     * @throws DuplicateAccountException 이미 존재하는 계좌번호인 경우
     */
    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request) {
        // 계좌번호 중복 검사
        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new DuplicateAccountException("이미 존재하는 계좌번호입니다.");
        }

        // 기본 설정으로 계좌 생성
        Account account = Account.builder()
                .accountNumber(request.getAccountNumber())
                .balance(BigDecimal.ZERO)
                .dailyWithdrawLimit(new BigDecimal("1000000"))  // 일일 출금 한도: 100만원
                .dailyTransferLimit(new BigDecimal("3000000"))  // 일일 이체 한도: 300만원
                .status(AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);
        return AccountResponse.from(savedAccount);
    }

    /**
     * 계좌를 삭제(비활성화)합니다.
     *
     * @param accountNumber 삭제할 계좌번호
     * @throws AccountNotFoundException 계좌를 찾을 수 없는 경우
     * @throws IllegalStateException 잔액이 있는 계좌를 삭제하려는 경우
     */
    @Transactional
    public void deleteAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("계좌를 찾을 수 없습니다."));

        // 잔액이 있는 계좌는 삭제 불가
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("잔액이 있는 계좌는 삭제할 수 없습니다.");
        }

        account.deactivate();
    }

    /**
     * 계좌 정보를 조회합니다.
     *
     * @param accountNumber 조회할 계좌번호
     * @return 계좌 정보
     * @throws AccountNotFoundException 계좌를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("계좌를 찾을 수 없습니다."));
        return AccountResponse.from(account);
    }

    /**
     * 계좌의 거래 내역을 조회합니다.
     *
     * @param accountNumber 조회할 계좌번호
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 거래 내역 정보
     * @throws AccountNotFoundException 계좌를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public TransactionHistoryResponse getTransactionHistory(String accountNumber, int page, int size) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("계좌를 찾을 수 없습니다."));

        // 거래일시 기준 내림차순 정렬
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transaction> transactionPage = transactionRepository
                .findByFromAccountOrToAccount(account, account, pageRequest);

        return TransactionHistoryResponse.from(
                accountNumber,
                transactionPage.getContent(),
                transactionPage.getTotalPages(),
                transactionPage.getTotalElements(),
                transactionPage.hasNext()
        );
    }
}
