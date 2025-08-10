package com.example.account.dto.response;

import com.example.account.entity.Transaction;
import lombok.Builder;
import lombok.Getter;
import com.example.account.entity.type.TransactionType;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class TransactionHistoryResponse {
    private String accountNumber;
    private List<TransactionDetail> transactions;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;

    @Getter
    @Builder
    public static class TransactionDetail {
        private String transactionId;
        private String type;
        private BigDecimal amount;
        private BigDecimal fee;
        private String counterPartyAccount;
        private Instant transactionDate;
        private String description;

        public static TransactionDetail from(Transaction transaction) {
            String counterPartyAccount = null;
            if (transaction.getType().equals(TransactionType.TRANSFER)) {
                counterPartyAccount = transaction.getToAccount().getAccountNumber();
            }

            return TransactionDetail.builder()
                    .transactionId(transaction.getTransactionId())
                    .type(transaction.getType().name())
                    .amount(transaction.getAmount())
                    .fee(transaction.getFee())
                    .counterPartyAccount(counterPartyAccount)
                    .transactionDate(transaction.getCreatedAt())
                    .description(transaction.getDescription())
                    .build();
        }
    }

    public static TransactionHistoryResponse from(
            String accountNumber,
            List<Transaction> transactions,
            int totalPages,
            long totalElements,
            boolean hasNext) {

        List<TransactionDetail> details = transactions.stream()
                .map(TransactionDetail::from)
                .collect(Collectors.toList());

        return TransactionHistoryResponse.builder()
                .accountNumber(accountNumber)
                .transactions(details)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .hasNext(hasNext)
                .build();
    }
}