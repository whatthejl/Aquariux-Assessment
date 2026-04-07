package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.entity.CryptoPair;
import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.entity.Trade;
import com.aquariux.technical.assessment.trade.entity.UserWallet;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.BadRequestException;
import com.aquariux.technical.assessment.trade.exception.InsufficientBalanceException;
import com.aquariux.technical.assessment.trade.exception.ResourceNotFoundException;
import com.aquariux.technical.assessment.trade.mapper.CryptoPairMapper;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import com.aquariux.technical.assessment.trade.mapper.TradeMapper;
import com.aquariux.technical.assessment.trade.mapper.UserWalletMapper;
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeServiceInterface {

    private static final int SCALE = 8;

    private final TradeMapper tradeMapper;
    private final CryptoPairMapper cryptoPairMapper;
    private final CryptoPriceMapper cryptoPriceMapper;
    private final UserWalletMapper userWalletMapper;

    // Add additional beans here if needed for your implementation

    @Override
    @Transactional
    public TradeResponse executeTrade(TradeRequest tradeRequest) {
        // TODO: Implement the core trading engine
        // What should happen when a user executes a trade?

//        validateRequest(tradeRequest);

        // Check User
        Long userId = tradeRequest.getUserId();
        if (!tradeMapper.existsUserById(userId)) {
            throw new ResourceNotFoundException("User not found for id: " + userId);
        }

        // Find pair
        CryptoPair pair = cryptoPairMapper.findByPairName(tradeRequest.getPairName());
        if (pair == null || Boolean.FALSE.equals(pair.getActive())) {
            throw new ResourceNotFoundException("Trading pair not found or inactive: " + tradeRequest.getPairName());
        }

        // Get latest price
        CryptoPrice latestPrice = cryptoPriceMapper.findLatestPriceByPairId(pair.getId());
        if (latestPrice == null) {
            throw new ResourceNotFoundException("Latest price not found for pair: " + tradeRequest.getPairName());
        }

        BigDecimal quantity = scale(tradeRequest.getQuantity());
        TradeType tradeType = tradeRequest.getTradeType();

        // Choose execution price
        BigDecimal executionPrice = tradeType == TradeType.BUY
                ? scale(latestPrice.getAskPrice())
                : scale(latestPrice.getBidPrice());

        if (executionPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Execution price must be greater than zero");
        }

        // Transaction amt
        BigDecimal totalAmount = scale(quantity.multiply(executionPrice));

        // BUY  = take money from quote wallet, put asset in base wallet
        // SELL = take asset from base wallet, put money in quote wallet
        Long debitSymbolId = tradeType == TradeType.BUY ? pair.getQuoteSymbolId() : pair.getBaseSymbolId();
        Long creditSymbolId = tradeType == TradeType.BUY ? pair.getBaseSymbolId() : pair.getQuoteSymbolId();
        String debitSymbol = tradeType == TradeType.BUY ? pair.getQuoteSymbol() : pair.getBaseSymbol();
        String creditSymbol = tradeType == TradeType.BUY ? pair.getBaseSymbol() : pair.getQuoteSymbol();

        BigDecimal debitAmount = tradeType == TradeType.BUY ? totalAmount : quantity;
        BigDecimal creditAmount = tradeType == TradeType.BUY ? quantity : totalAmount;

        // Check user money
        UserWallet debitWallet = userWalletMapper.findWalletByUserIdAndSymbolId(userId, debitSymbolId);
        if (debitWallet == null) {
            throw new InsufficientBalanceException("Insufficient balance in " + debitSymbol + " wallet");
        }

        if (debitWallet.getBalance() == null || debitWallet.getBalance().compareTo(debitAmount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in " + debitSymbol + " wallet");
        }

        // Update user money
        BigDecimal updatedDebitBalance = scale(debitWallet.getBalance().subtract(debitAmount));
        debitWallet.setBalance(updatedDebitBalance);
        userWalletMapper.updateWalletBalance(debitWallet);

        // Update user wallet, create if not existing
        UserWallet creditWallet = userWalletMapper.findWalletByUserIdAndSymbolId(userId, creditSymbolId);
        BigDecimal updatedCreditBalance;
        if (creditWallet == null) {
            creditWallet = new UserWallet();
            creditWallet.setUserId(userId);
            creditWallet.setSymbolId(creditSymbolId);
            creditWallet.setBalance(scale(creditAmount));
            userWalletMapper.insertWallet(creditWallet);
            updatedCreditBalance = creditWallet.getBalance();
        } else {
            updatedCreditBalance = scale(creditWallet.getBalance().add(creditAmount));
            creditWallet.setBalance(updatedCreditBalance);
            userWalletMapper.updateWalletBalance(creditWallet);
        }

        LocalDateTime tradeTime = LocalDateTime.now();

        Trade trade = new Trade();
        trade.setUserId(userId);
        trade.setCryptoPairId(pair.getId());
        trade.setTradeType(tradeType.name());
        trade.setQuantity(quantity);
        trade.setPrice(executionPrice);
        trade.setTotalAmount(totalAmount);
        trade.setTradeTime(tradeTime);

        tradeMapper.insertTrade(trade);

        TradeResponse response = new TradeResponse();
        response.setTradeId(trade.getId());
        response.setUserId(userId);
        response.setPairName(pair.getPairName());
        response.setTradeType(tradeType);
        response.setQuantity(quantity);
        response.setPrice(executionPrice);
        response.setTotalAmount(totalAmount);
        response.setDebitSymbol(debitSymbol);
        response.setCreditSymbol(creditSymbol);
        response.setDebitAmount(scale(debitAmount));
        response.setCreditAmount(scale(creditAmount));
        response.setDebitBalance(updatedDebitBalance);
        response.setCreditBalance(updatedCreditBalance);
        response.setTradeTime(tradeTime);
        response.setStatus("SUCCESS");
        return response;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}