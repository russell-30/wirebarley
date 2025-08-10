package com.example.account.controller;

import com.example.account.dto.request.AccountCreateRequest;
import com.example.account.dto.response.AccountResponse;
import com.example.account.dto.response.TransactionHistoryResponse;
import com.example.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Account", description = "계좌 관리 API")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "계좌 생성", description = "새로운 계좌를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "계좌 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @RequestBody AccountCreateRequest request) {
        return ResponseEntity.ok(accountService.createAccount(request));
    }

    @Operation(summary = "계좌 조회", description = "계좌 번호로 계좌 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "계좌 조회 성공"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(
            @Parameter(description = "계좌번호", required = true)
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @Operation(summary = "계좌 삭제", description = "계좌 번호로 계좌를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "계좌 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<Void> deleteAccount(
            @Parameter(description = "계좌번호", required = true)
            @PathVariable String accountNumber) {
        accountService.deleteAccount(accountNumber);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "거래 내역 조회", description = "계좌의 거래 내역을 페이지 단위로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거래 내역 조회 성공"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<TransactionHistoryResponse> getTransactionHistory(
            @Parameter(description = "계좌번호", required = true)
            @PathVariable String accountNumber,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(accountService.getTransactionHistory(accountNumber, page, size));
    }
}