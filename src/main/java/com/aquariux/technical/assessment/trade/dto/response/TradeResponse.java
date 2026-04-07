package com.aquariux.technical.assessment.trade.dto.response;

import com.aquariux.technical.assessment.trade.enums.TradeType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TradeResponse {
    // TODO: What should you return after a trade is executed?
    private Long tradeId;
    private Long userId;
    private String pairName;
    private TradeType tradeType;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private String debitSymbol;
    private String creditSymbol;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal debitBalance;
    private BigDecimal creditBalance;
    private LocalDateTime tradeTime;
    private String status;
}