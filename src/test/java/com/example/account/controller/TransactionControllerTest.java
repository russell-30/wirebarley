package com.example.account.controller;

import com.example.account.dto.request.TransactionRequest;
import com.example.account.dto.request.TransferRequest;
import com.example.account.dto.response.TransactionResponse;
import com.example.account.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@MockBean(JpaMetamodelMappingContext.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    void deposit_Success() throws Exception {
        // given
        TransactionRequest request = TransactionRequest.builder()
                .accountNumber("1234567890")
                .amount(BigDecimal.valueOf(1000))
                .build();

        TransactionResponse response = TransactionResponse.builder()
                .transactionId("DEP123")
                .toAccount("1234567890")
                .amount(BigDecimal.valueOf(1000))
                .fee(BigDecimal.ZERO)
                .type("DEPOSIT")
                .status("COMPLETED")
                .createdAt(Instant.now())
                .build();

        // when
        given(transactionService.deposit(any())).willReturn(response);

        // then
        mockMvc.perform(post("/api/transactions/deposit")
                        .with(csrf())
                        .with(user("testUser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("DEP123"))
                .andExpect(jsonPath("$.toAccount").value("1234567890"))
                .andExpect(jsonPath("$.amount").value(1000))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andDo(print());
    }

    @Test
    void withdraw_Success() throws Exception {
        // given
        TransactionRequest request = TransactionRequest.builder()
                .accountNumber("1234567890")
                .amount(BigDecimal.valueOf(1000))
                .build();

        TransactionResponse response = TransactionResponse.builder()
                .transactionId("WD123")
                .fromAccount("1234567890")
                .amount(BigDecimal.valueOf(1000))
                .fee(BigDecimal.ZERO)
                .type("WITHDRAW")
                .status("COMPLETED")
                .createdAt(Instant.now())
                .build();

        // when
        given(transactionService.withdraw(any())).willReturn(response);

        // then
        mockMvc.perform(post("/api/transactions/withdraw")
                        .with(csrf())
                        .with(user("testUser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("WD123"))
                .andExpect(jsonPath("$.fromAccount").value("1234567890"))
                .andExpect(jsonPath("$.amount").value(1000))
                .andExpect(jsonPath("$.type").value("WITHDRAW"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andDo(print());
    }

    @Test
    void transfer_Success() throws Exception {
        // given
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("1234567890")
                .toAccountNumber("0987654321")
                .amount(BigDecimal.valueOf(1000))
                .build();

        TransactionResponse response = TransactionResponse.builder()
                .transactionId("TX123")
                .fromAccount("1234567890")
                .toAccount("0987654321")
                .amount(BigDecimal.valueOf(1000))
                .fee(BigDecimal.ZERO)
                .type("TRANSFER")
                .status("COMPLETED")
                .createdAt(Instant.now())
                .build();

        // Mockito 설정 수정
        given(transactionService.transfer(any(TransferRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/transactions/transfer")
                        .with(csrf())
                        .with(user("testUser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TX123"))
                .andExpect(jsonPath("$.fromAccount").value("1234567890"))
                .andExpect(jsonPath("$.toAccount").value("0987654321"))
                .andExpect(jsonPath("$.amount").value(1000))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andDo(print());
    }
}