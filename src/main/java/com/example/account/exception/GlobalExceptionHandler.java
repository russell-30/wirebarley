package com.example.account.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = bindingResult.getFieldErrors()
                .get(0)
                .getDefaultMessage();

        log.error("ValidationException: {}", message, e);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.INVALID_TRANSACTION, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("UnhandledException: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(ErrorCode.SYSTEM_ERROR, "시스템 오류가 발생했습니다."));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalanceException(InsufficientBalanceException e) {
        log.error("InsufficientBalanceException: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.INSUFFICIENT_BALANCE.getStatus())
                .body(new ErrorResponse(ErrorCode.INSUFFICIENT_BALANCE, e.getMessage()));
    }

    @ExceptionHandler(DailyLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleDailyLimitExceededException(DailyLimitExceededException e) {
        log.error("DailyLimitExceededException: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.DAILY_LIMIT_EXCEEDED.getStatus())
                .body(new ErrorResponse(ErrorCode.DAILY_LIMIT_EXCEEDED, e.getMessage()));
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotActiveException(AccountNotActiveException e) {
        return ResponseEntity.status(ErrorCode.ACCOUNT_NOT_ACTIVE.getStatus())
                .body(new ErrorResponse(ErrorCode.ACCOUNT_NOT_ACTIVE, e.getMessage()));
    }

}