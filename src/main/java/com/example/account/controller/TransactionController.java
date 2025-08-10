package com.example.account.controller;

import com.example.account.dto.request.TransactionRequest;
import com.example.account.dto.request.TransferRequest;
import com.example.account.dto.response.TransactionResponse;
import com.example.account.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 계좌 거래 관련 API를 처리하는 컨트롤러
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "거래 API", description = "입금, 출금, 계좌이체 관련 API")
public class TransactionController {
    private final TransactionService transactionService;

    /**
     * 계좌 입금을 처리합니다.
     *
     * @param request 입금 요청 정보 (계좌번호, 금액)
     * @return 거래 처리 결과
     */
    @Operation(summary = "계좌 입금", description = "지정된 계좌에 금액을 입금합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "입금 성공",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Parameter(description = "입금 요청 정보", required = true)
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.deposit(request));
    }

    /**
     * 계좌 출금을 처리합니다.
     *
     * @param request 출금 요청 정보 (계좌번호, 금액)
     * @return 거래 처리 결과
     */
    @Operation(summary = "계좌 출금", description = "지정된 계좌에서 금액을 출금합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "출금 성공"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "잔액 부족 또는 한도 초과")
    })
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Parameter(description = "출금 요청 정보", required = true)
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.withdraw(request));
    }

    /**
     * 계좌 이체를 처리합니다.
     *
     * @param request 이체 요청 정보 (출금계좌, 입금계좌, 금액)
     * @return 거래 처리 결과
     */
    @Operation(summary = "계좌 이체", description = "한 계좌에서 다른 계좌로 금액을 이체합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이체 성공"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "잔액 부족 또는 한도 초과")
    })
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Parameter(description = "이체 요청 정보", required = true)
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transactionService.transfer(request));
    }
}