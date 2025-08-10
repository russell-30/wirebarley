package com.example.account.dto.response;

import com.example.account.entity.Account;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class AccountResponse {
    private String accountNumber;
    private BigDecimal balance;
    private String status;
    private Instant createdAt;

    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .status(account.getStatus().name())
                .createdAt(account.getCreatedAt())
                .build();
    }
}