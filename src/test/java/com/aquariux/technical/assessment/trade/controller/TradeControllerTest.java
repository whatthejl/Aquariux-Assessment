package com.aquariux.technical.assessment.trade.controller;

import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = TradeController.class)
@AutoConfigureMockMvc(addFilters = false)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeServiceInterface tradeService;

    @Test
    @DisplayName("POST /api/trades/execute should return 400 when pairName is blank")
    void executeTrade_shouldReturnBadRequestWhenPairNameIsBlank() throws Exception {
        String requestBody = """
                {
                  "userId": 1,
                  "tradeType": "BUY",
                  "pairName": " ",
                  "quantity": 0.1
                }
                """;

        mockMvc.perform(post("/api/trades/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("pairName")));

        verifyNoInteractions(tradeService);
    }

    @Test
    @DisplayName("POST /api/trades/execute should return 400 when quantity is zero")
    void executeTrade_shouldReturnBadRequestWhenQuantityIsZero() throws Exception {
        String requestBody = """
                {
                  "userId": 1,
                  "tradeType": "BUY",
                  "pairName": "BTCUSDT",
                  "quantity": 0
                }
                """;

        mockMvc.perform(post("/api/trades/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("quantity")));

        verifyNoInteractions(tradeService);
    }

    @Test
    @DisplayName("POST /api/trades/execute should return 400 when quantity is negative")
    void executeTrade_shouldReturnBadRequestWhenQuantityIsNegative() throws Exception {
        String requestBody = """
                {
                  "userId": 1,
                  "tradeType": "BUY",
                  "pairName": "BTCUSDT",
                  "quantity": -0.1
                }
                """;

        mockMvc.perform(post("/api/trades/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("quantity")));

        verifyNoInteractions(tradeService);
    }

    @Test
    @DisplayName("POST /api/trades/execute should return 400 when userId is missing")
    void executeTrade_shouldReturnBadRequestWhenUserIdIsMissing() throws Exception {
        String requestBody = """
                {
                  "tradeType": "BUY",
                  "pairName": "BTCUSDT",
                  "quantity": 0.1
                }
                """;

        mockMvc.perform(post("/api/trades/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("userId")));

        verifyNoInteractions(tradeService);
    }

    @Test
    @DisplayName("POST /api/trades/execute should return 400 when tradeType is missing")
    void executeTrade_shouldReturnBadRequestWhenTradeTypeIsMissing() throws Exception {
        String requestBody = """
                {
                  "userId": 1,
                  "pairName": "BTCUSDT",
                  "quantity": 0.1
                }
                """;

        mockMvc.perform(post("/api/trades/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("tradeType")));

        verifyNoInteractions(tradeService);
    }

    @Test
    @DisplayName("POST /api/trades/execute should return 400 when pairName is missing")
    void executeTrade_shouldReturnBadRequestWhenPairNameIsMissing() throws Exception {
        String requestBody = """
                {
                  "userId": 1,
                  "tradeType": "BUY",
                  "quantity": 0.1
                }
                """;

        mockMvc.perform(post("/api/trades/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("pairName")));

        verifyNoInteractions(tradeService);
    }

    @Test
    @DisplayName("POST /api/trades/execute should return 400 when quantity is missing")
    void executeTrade_shouldReturnBadRequestWhenQuantityIsMissing() throws Exception {
        String requestBody = """
                {
                  "userId": 1,
                  "tradeType": "BUY",
                  "pairName": "BTCUSDT"
                }
                """;

        mockMvc.perform(post("/api/trades/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("quantity")));

        verifyNoInteractions(tradeService);
    }

    @Test
    @DisplayName("POST /api/trades/execute should return 200 for a valid request")
    void executeTrade_shouldReturnSuccessForValidRequest() throws Exception {
        TradeResponse response = new TradeResponse();
        response.setTradeId(999L);
        response.setUserId(1L);
        response.setPairName("BTCUSDT");
        response.setTradeType(TradeType.BUY);
        response.setQuantity(new BigDecimal("0.10000000"));
        response.setPrice(new BigDecimal("50100.00000000"));
        response.setTotalAmount(new BigDecimal("5010.00000000"));
        response.setDebitSymbol("USDT");
        response.setCreditSymbol("BTC");
        response.setDebitAmount(new BigDecimal("5010.00000000"));
        response.setCreditAmount(new BigDecimal("0.10000000"));
        response.setDebitBalance(new BigDecimal("4990.00000000"));
        response.setCreditBalance(new BigDecimal("1.10000000"));
        response.setTradeTime(LocalDateTime.of(2026, 4, 7, 10, 0, 0));
        response.setStatus("SUCCESS");

        when(tradeService.executeTrade(any())).thenReturn(response);

        String requestBody = """
                {
                  "userId": 1,
                  "tradeType": "BUY",
                  "pairName": "BTCUSDT",
                  "quantity": 0.1
                }
                """;

        mockMvc.perform(post("/api/trades/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId").value(999))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.pairName").value("BTCUSDT"))
                .andExpect(jsonPath("$.tradeType").value("BUY"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}