package com.example.account.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."),
    ACCOUNT_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "활성화 된 계좌가 아닙니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "잔액이 부족합니다."),
    DAILY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "일일 한도를 초과했습니다."),
    DUPLICATE_ACCOUNT(HttpStatus.BAD_REQUEST, "이미 존재하는 계좌번호입니다."),
    INVALID_TRANSACTION(HttpStatus.BAD_REQUEST, "유효하지 않은 거래입니다."),
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "시스템 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}