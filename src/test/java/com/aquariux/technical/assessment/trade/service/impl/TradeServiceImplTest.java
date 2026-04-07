package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.entity.CryptoPair;
import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.entity.Trade;
import com.aquariux.technical.assessment.trade.entity.UserWallet;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.InsufficientBalanceException;
import com.aquariux.technical.assessment.trade.exception.ResourceNotFoundException;
import com.aquariux.technical.assessment.trade.mapper.CryptoPairMapper;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import com.aquariux.technical.assessment.trade.mapper.TradeMapper;
import com.aquariux.technical.assessment.trade.mapper.UserWalletMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceImplTest {

    @Mock
    private TradeMapper tradeMapper;

    @Mock
    private CryptoPairMapper cryptoPairMapper;

    @Mock
    private CryptoPriceMapper cryptoPriceMapper;

    @Mock
    private UserWalletMapper userWalletMapper;

    @InjectMocks
    private TradeServiceImpl tradeService;

    private TradeRequest buyRequest;
    private TradeRequest sellRequest;
    private CryptoPair btcUsdtPair;
    private CryptoPrice latestPrice;
    private UserWallet usdtWallet;
    private UserWallet btcWallet;

    /**
     * TEST SETUP (shared across all test cases)
     *
     * This method initializes a consistent test environment for all scenarios.
     * It simulates a user trading BTC against USDT using predefined market data and wallet balances.
     *
     * --------------------------------------------------
     * USER CONTEXT
     * --------------------------------------------------
     * - userId: 1
     * - Assumed to exist in the system (mocked via tradeMapper.existsUserById)
     *
     * --------------------------------------------------
     * TRADE REQUESTS
     * --------------------------------------------------
     * BUY REQUEST:
     * - pair: BTCUSDT
     * - quantity: 0.1 BTC
     * - meaning: user wants to BUY 0.1 BTC using USDT
     *
     * SELL REQUEST:
     * - pair: BTCUSDT
     * - quantity: 0.1 BTC
     * - meaning: user wants to SELL 0.1 BTC to receive USDT
     *
     * --------------------------------------------------
     * CRYPTO PAIR CONFIGURATION
     * --------------------------------------------------
     * Pair: BTCUSDT
     * - base symbol: BTC (id = 1)
     * - quote symbol: USDT (id = 3)
     *
     * Interpretation:
     * - BUY BTCUSDT → spend USDT, receive BTC
     * - SELL BTCUSDT → spend BTC, receive USDT
     *
     * --------------------------------------------------
     * MARKET PRICE DATA (LATEST PRICE)
     * --------------------------------------------------
     * - bid price: 50000  → used for SELL trades
     * - ask price: 50100  → used for BUY trades
     *
     * Key rule:
     * - BUY uses ask price
     * - SELL uses bid price
     *
     * --------------------------------------------------
     * WALLET STATE BEFORE EACH TEST
     * --------------------------------------------------
     * USDT Wallet:
     * - symbolId: 3
     * - balance: 10000 USDT
     *
     * BTC Wallet:
     * - symbolId: 1
     * - balance: 1.0 BTC
     *
     * These values are used to simulate sufficient balance for both BUY and SELL scenarios.
     *
     * --------------------------------------------------
     * PURPOSE OF THIS SETUP
     * --------------------------------------------------
     * - Provide a consistent baseline for all test cases
     * - Avoid duplication of object creation
     * - Ensure predictable and verifiable trade outcomes
     * - Allow each test to focus only on its specific behavior
     *
     * --------------------------------------------------
     * IMPORTANT ASSUMPTIONS
     * --------------------------------------------------
     * - No fees are applied
     * - No slippage (price is fixed from latestPrice)
     * - Trades are executed immediately (market order)
     * - Precision is handled using BigDecimal (8 decimal places)
     */
    @BeforeEach
    void setUp() {
        buyRequest = new TradeRequest();
        buyRequest.setUserId(1L);
        buyRequest.setTradeType(TradeType.BUY);
        buyRequest.setPairName("BTCUSDT");
        buyRequest.setQuantity(new BigDecimal("0.10000000"));

        sellRequest = new TradeRequest();
        sellRequest.setUserId(1L);
        sellRequest.setTradeType(TradeType.SELL);
        sellRequest.setPairName("BTCUSDT");
        sellRequest.setQuantity(new BigDecimal("0.10000000"));

        btcUsdtPair = new CryptoPair();
        btcUsdtPair.setId(10L);
        btcUsdtPair.setPairName("BTCUSDT");
        btcUsdtPair.setBaseSymbolId(1L);   // BTC
        btcUsdtPair.setQuoteSymbolId(3L);  // USDT
        btcUsdtPair.setBaseSymbol("BTC");
        btcUsdtPair.setQuoteSymbol("USDT");
        btcUsdtPair.setActive(true);

        latestPrice = new CryptoPrice();
        latestPrice.setId(100L);
        latestPrice.setCryptoPairId(10L);
        latestPrice.setBidPrice(new BigDecimal("50000.00000000"));
        latestPrice.setAskPrice(new BigDecimal("50100.00000000"));

        usdtWallet = new UserWallet();
        usdtWallet.setId(1000L);
        usdtWallet.setUserId(1L);
        usdtWallet.setSymbolId(3L);
        usdtWallet.setBalance(new BigDecimal("10000.00000000"));

        btcWallet = new UserWallet();
        btcWallet.setId(1001L);
        btcWallet.setUserId(1L);
        btcWallet.setSymbolId(1L);
        btcWallet.setBalance(new BigDecimal("1.00000000"));
    }

    /**
     * SCENARIO: Successful BUY trade (BTCUSDT)
     *
     * INPUT:
     * - userId: 1
     * - tradeType: BUY
     * - pairName: BTCUSDT
     * - quantity: 0.1 BTC
     *
     * MARKET DATA:
     * - bid price = 50000 (used for SELL)
     * - ask price = 50100 (used for BUY)
     *
     * WALLET STATE BEFORE:
     * - USDT wallet: 10000
     * - BTC wallet: 1.0
     *
     * EXPECTED BUSINESS LOGIC:
     * - BUY uses ASK price → 50100
     * - total cost = 0.1 × 50100 = 5010 USDT
     * - debit USDT wallet by 5010
     * - credit BTC wallet by 0.1
     *
     * WALLET STATE AFTER:
     * - USDT: 10000 - 5010 = 4990
     * - BTC: 1.0 + 0.1 = 1.1
     *
     * EXPECTED RESULT:
     * - Trade is successfully created
     * - Trade saved with:
     *   - price = 50100
     *   - totalAmount = 5010
     *   - quantity = 0.1
     * - Response contains correct debit/credit symbols and updated balances
     *
     * VERIFICATIONS:
     * - tradeMapper.insertTrade() is called
     * - wallet balances are updated twice (debit + credit)
     * - no new wallet is created
     */
    @Test
    void executeTrade_buy_shouldDebitUsdtCreditBtcAndSaveTrade() {
        when(tradeMapper.existsUserById(1L)).thenReturn(true);
        when(cryptoPairMapper.findByPairName("BTCUSDT")).thenReturn(btcUsdtPair);
        when(cryptoPriceMapper.findLatestPriceByPairId(10L)).thenReturn(latestPrice);

        when(userWalletMapper.findWalletByUserIdAndSymbolId(1L, 3L)).thenReturn(usdtWallet); // debit wallet
        when(userWalletMapper.findWalletByUserIdAndSymbolId(1L, 1L)).thenReturn(btcWallet);  // credit wallet

        doAnswer(invocation -> {
            Trade trade = invocation.getArgument(0);
            trade.setId(999L);
            return 1;
        }).when(tradeMapper).insertTrade(any(Trade.class));

        TradeResponse response = tradeService.executeTrade(buyRequest);

        assertThat(response.getTradeId()).isEqualTo(999L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getPairName()).isEqualTo("BTCUSDT");
        assertThat(response.getTradeType()).isEqualTo(TradeType.BUY);
        assertThat(response.getPrice()).isEqualByComparingTo("50100.00000000");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("5010.00000000");

        assertThat(response.getDebitSymbol()).isEqualTo("USDT");
        assertThat(response.getCreditSymbol()).isEqualTo("BTC");
        assertThat(response.getDebitAmount()).isEqualByComparingTo("5010.00000000");
        assertThat(response.getCreditAmount()).isEqualByComparingTo("0.10000000");

        assertThat(response.getDebitBalance()).isEqualByComparingTo("4990.00000000");
        assertThat(response.getCreditBalance()).isEqualByComparingTo("1.10000000");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeMapper).insertTrade(tradeCaptor.capture());

        Trade savedTrade = tradeCaptor.getValue();
        assertThat(savedTrade.getUserId()).isEqualTo(1L);
        assertThat(savedTrade.getCryptoPairId()).isEqualTo(10L);
        assertThat(savedTrade.getTradeType()).isEqualTo("BUY");
        assertThat(savedTrade.getQuantity()).isEqualByComparingTo("0.10000000");
        assertThat(savedTrade.getPrice()).isEqualByComparingTo("50100.00000000");
        assertThat(savedTrade.getTotalAmount()).isEqualByComparingTo("5010.00000000");

        verify(userWalletMapper, times(2)).updateWalletBalance(any(UserWallet.class));
        verify(userWalletMapper, never()).insertWallet(any(UserWallet.class));
    }

    /**
     * SCENARIO: Successful SELL trade (BTCUSDT)
     *
     * INPUT:
     * - userId: 1
     * - tradeType: SELL
     * - pairName: BTCUSDT
     * - quantity: 0.1 BTC
     *
     * MARKET DATA:
     * - bid price = 50000 (used for SELL)
     * - ask price = 50100
     *
     * WALLET STATE BEFORE:
     * - BTC wallet: 1.0
     * - USDT wallet: 10000
     *
     * EXPECTED BUSINESS LOGIC:
     * - SELL uses BID price → 50000
     * - total proceeds = 0.1 × 50000 = 5000 USDT
     * - debit BTC wallet by 0.1
     * - credit USDT wallet by 5000
     *
     * WALLET STATE AFTER:
     * - BTC: 1.0 - 0.1 = 0.9
     * - USDT: 10000 + 5000 = 15000
     *
     * EXPECTED RESULT:
     * - Trade is successfully created
     * - Trade saved with:
     *   - price = 50000
     *   - totalAmount = 5000
     * - Response reflects correct debit/credit and balances
     *
     * VERIFICATIONS:
     * - tradeMapper.insertTrade() is called
     * - wallet balances are updated twice
     * - no wallet creation occurs
     */
    @Test
    void executeTrade_sell_shouldDebitBtcCreditUsdtAndSaveTrade() {
        when(tradeMapper.existsUserById(1L)).thenReturn(true);
        when(cryptoPairMapper.findByPairName("BTCUSDT")).thenReturn(btcUsdtPair);
        when(cryptoPriceMapper.findLatestPriceByPairId(10L)).thenReturn(latestPrice);

        when(userWalletMapper.findWalletByUserIdAndSymbolId(1L, 1L)).thenReturn(btcWallet);  // debit wallet
        when(userWalletMapper.findWalletByUserIdAndSymbolId(1L, 3L)).thenReturn(usdtWallet); // credit wallet

        doAnswer(invocation -> {
            Trade trade = invocation.getArgument(0);
            trade.setId(1000L);
            return 1;
        }).when(tradeMapper).insertTrade(any(Trade.class));

        TradeResponse response = tradeService.executeTrade(sellRequest);

        assertThat(response.getTradeId()).isEqualTo(1000L);
        assertThat(response.getTradeType()).isEqualTo(TradeType.SELL);
        assertThat(response.getPrice()).isEqualByComparingTo("50000.00000000");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("5000.00000000");

        assertThat(response.getDebitSymbol()).isEqualTo("BTC");
        assertThat(response.getCreditSymbol()).isEqualTo("USDT");
        assertThat(response.getDebitAmount()).isEqualByComparingTo("0.10000000");
        assertThat(response.getCreditAmount()).isEqualByComparingTo("5000.00000000");

        assertThat(response.getDebitBalance()).isEqualByComparingTo("0.90000000");
        assertThat(response.getCreditBalance()).isEqualByComparingTo("15000.00000000");

        verify(userWalletMapper, times(2)).updateWalletBalance(any(UserWallet.class));
        verify(userWalletMapper, never()).insertWallet(any(UserWallet.class));
    }

    /**
     * SCENARIO: BUY trade where destination wallet does NOT exist
     *
     * INPUT:
     * - userId: 1
     * - tradeType: BUY
     * - pairName: BTCUSDT
     * - quantity: 0.1 BTC
     *
     * MARKET DATA:
     * - ask price = 50100
     *
     * WALLET STATE BEFORE:
     * - USDT wallet: 10000
     * - BTC wallet: DOES NOT EXIST
     *
     * EXPECTED BUSINESS LOGIC:
     * - BUY uses ASK price → 50100
     * - total cost = 0.1 × 50100 = 5010 USDT
     * - debit USDT wallet by 5010
     * - create new BTC wallet
     * - credit BTC wallet with 0.1
     *
     * WALLET STATE AFTER:
     * - USDT: 10000 - 5010 = 4990
     * - BTC: newly created with 0.1
     *
     * EXPECTED RESULT:
     * - Trade is successfully created
     * - BTC wallet is inserted (new record)
     * - USDT wallet is updated
     *
     * VERIFICATIONS:
     * - updateWalletBalance() called once (for USDT)
     * - insertWallet() called once (for BTC)
     * - tradeMapper.insertTrade() called
     */
    @Test
    void executeTrade_buy_shouldCreateDestinationWalletWhenMissing() {
        when(tradeMapper.existsUserById(1L)).thenReturn(true);
        when(cryptoPairMapper.findByPairName("BTCUSDT")).thenReturn(btcUsdtPair);
        when(cryptoPriceMapper.findLatestPriceByPairId(10L)).thenReturn(latestPrice);

        when(userWalletMapper.findWalletByUserIdAndSymbolId(1L, 3L)).thenReturn(usdtWallet); // debit wallet
        when(userWalletMapper.findWalletByUserIdAndSymbolId(1L, 1L)).thenReturn(null);       // no BTC wallet yet

        doAnswer(invocation -> {
            UserWallet wallet = invocation.getArgument(0);
            wallet.setId(2000L);
            return 1;
        }).when(userWalletMapper).insertWallet(any(UserWallet.class));

        doAnswer(invocation -> {
            Trade trade = invocation.getArgument(0);
            trade.setId(3000L);
            return 1;
        }).when(tradeMapper).insertTrade(any(Trade.class));

        TradeResponse response = tradeService.executeTrade(buyRequest);

        assertThat(response.getDebitBalance()).isEqualByComparingTo("4990.00000000");
        assertThat(response.getCreditBalance()).isEqualByComparingTo("0.10000000");

        verify(userWalletMapper, times(1)).updateWalletBalance(any(UserWallet.class));
        verify(userWalletMapper, times(1)).insertWallet(any(UserWallet.class));
    }

    @Test
    void executeTrade_shouldThrowWhenUserNotFound() {
        when(tradeMapper.existsUserById(1L)).thenReturn(false);

        assertThatThrownBy(() -> tradeService.executeTrade(buyRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void executeTrade_shouldThrowWhenPairNotFound() {
        when(tradeMapper.existsUserById(1L)).thenReturn(true);
        when(cryptoPairMapper.findByPairName("BTCUSDT")).thenReturn(null);

        assertThatThrownBy(() -> tradeService.executeTrade(buyRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Trading pair not found");
    }

    @Test
    void executeTrade_shouldThrowWhenLatestPriceNotFound() {
        when(tradeMapper.existsUserById(1L)).thenReturn(true);
        when(cryptoPairMapper.findByPairName("BTCUSDT")).thenReturn(btcUsdtPair);
        when(cryptoPriceMapper.findLatestPriceByPairId(10L)).thenReturn(null);

        assertThatThrownBy(() -> tradeService.executeTrade(buyRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Latest price not found");
    }

    @Test
    void executeTrade_shouldThrowWhenDebitWalletMissing() {
        when(tradeMapper.existsUserById(1L)).thenReturn(true);
        when(cryptoPairMapper.findByPairName("BTCUSDT")).thenReturn(btcUsdtPair);
        when(cryptoPriceMapper.findLatestPriceByPairId(10L)).thenReturn(latestPrice);
        when(userWalletMapper.findWalletByUserIdAndSymbolId(1L, 3L)).thenReturn(null);

        assertThatThrownBy(() -> tradeService.executeTrade(buyRequest))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void executeTrade_shouldThrowWhenBalanceIsNotEnough() {
        usdtWallet.setBalance(new BigDecimal("1000.00000000"));

        when(tradeMapper.existsUserById(1L)).thenReturn(true);
        when(cryptoPairMapper.findByPairName("BTCUSDT")).thenReturn(btcUsdtPair);
        when(cryptoPriceMapper.findLatestPriceByPairId(10L)).thenReturn(latestPrice);
        when(userWalletMapper.findWalletByUserIdAndSymbolId(1L, 3L)).thenReturn(usdtWallet);

        assertThatThrownBy(() -> tradeService.executeTrade(buyRequest))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");
    }
}