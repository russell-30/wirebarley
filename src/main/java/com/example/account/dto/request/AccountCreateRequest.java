package com.example.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountCreateRequest {
    @NotBlank(message = "계좌번호는 필수입니다.")
    @Pattern(regexp = "^\\d{10}$", message = "계좌번호는 10자리 숫자여야 합니다.")
    private String accountNumber;
}