package com.example.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {
    @NotBlank(message = "계좌번호는 필수입니다.")
    private String accountNumber;

    @Positive(message = "거래금액은 0보다 커야 합니다.")
    private BigDecimal amount;
}