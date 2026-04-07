package com.aquariux.technical.assessment.trade.dto.request;

import com.aquariux.technical.assessment.trade.enums.TradeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "tradeType is required")
    private TradeType tradeType;
    
    // TODO: What information do you need to execute a trade?

    @NotBlank(message = "pairName is required")
    private String pairName; // Which pair is being traded, like BTCUSDT

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than zero")
    private BigDecimal quantity;
}