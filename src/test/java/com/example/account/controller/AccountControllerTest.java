package com.example.account.controller;

import com.example.account.dto.request.AccountCreateRequest;
import com.example.account.dto.response.AccountResponse;
import com.example.account.dto.response.TransactionHistoryResponse;
import com.example.account.dto.response.TransactionResponse;
import com.example.account.entity.Account;
import com.example.account.entity.Transaction;
import static org.mockito.Mockito.when;

import com.example.account.entity.type.AccountStatus;
import com.example.account.service.AccountService;
import com.example.account.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@MockBean(JpaMetamodelMappingContext.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @Test
    void createAccount_Success() throws Exception {
        // given
        AccountCreateRequest request = new AccountCreateRequest();
        request.setAccountNumber("1234567890");

        AccountResponse response = AccountResponse.builder()
                .accountNumber("1234567890")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE.name())
                .createdAt(Instant.now())
                .build();

        given(accountService.createAccount(any(AccountCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/accounts")
                        .with(csrf())
                        .with(user("testUser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.balance").value("0"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getAccount_Success() throws Exception {
        // given
        String accountNumber = "1234567890";
        AccountResponse response = AccountResponse.builder()
                .accountNumber(accountNumber)
                .balance(BigDecimal.valueOf(1000))
                .status(AccountStatus.ACTIVE.name())
                .createdAt(Instant.now())
                .build();

        given(accountService.getAccount(accountNumber)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/accounts/{accountNumber}", accountNumber)
                        .with(csrf())
                        .with(user("testUser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(accountNumber))
                .andExpect(jsonPath("$.balance").value("1000"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deleteAccount_Success() throws Exception {
        // given
        String accountNumber = "1234567890";
        doNothing().when(accountService).deleteAccount(accountNumber);

        // when & then
        mockMvc.perform(delete("/api/accounts/{accountNumber}", accountNumber)
                        .with(csrf())
                        .with(user("testUser").roles("USER")))
                .andExpect(status().isNoContent());
    }

    @Test
    void getTransactionHistory_Success() throws Exception {
        // given
        String accountNumber = "1234567890";
        List<TransactionHistoryResponse.TransactionDetail> details = Arrays.asList(
                TransactionHistoryResponse.TransactionDetail.builder()
                        .transactionId("TX001")
                        .type("TRANSFER")
                        .amount(BigDecimal.valueOf(1000))
                        .build()
        );

        TransactionHistoryResponse response = TransactionHistoryResponse.builder()
                .accountNumber(accountNumber)
                .transactions(details)
                .totalPages(1)
                .totalElements(1)
                .hasNext(false)
                .build();

        when(accountService.getTransactionHistory(eq(accountNumber), eq(0), eq(20)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/accounts/{accountNumber}/transactions", accountNumber)
                        .with(csrf())
                        .with(user("testUser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(accountNumber));
    }
}