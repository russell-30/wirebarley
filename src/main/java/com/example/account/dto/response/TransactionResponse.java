package com.example.account.dto.response;

import com.example.account.entity.Transaction;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class TransactionResponse {
    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private BigDecimal fee;
    private String type;
    private String status;
    private Instant createdAt;

    public static TransactionResponse from(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .fromAccount(transaction.getFromAccount() != null ?
                        transaction.getFromAccount().getAccountNumber() : null)
                .toAccount(transaction.getToAccount() != null ?
                        transaction.getToAccount().getAccountNumber() : null)
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}