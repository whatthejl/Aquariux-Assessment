package com.aquariux.technical.assessment.trade.mapper;

import com.aquariux.technical.assessment.trade.entity.Trade;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TradeMapper {
    
    // TODO: What database operations do you need for trading?
    // Feel free to add multiple methods, complex queries, or additional mapper interfaces as needed
    @Select("""
            SELECT COUNT(1) > 0
            FROM users
            WHERE id = #{userId}
            """)
    boolean existsUserById(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO trades (
                user_id,
                crypto_pair_id,
                trade_type,
                quantity,
                price,
                total_amount,
                trade_time
            )
            VALUES (
                #{userId},
                #{cryptoPairId},
                #{tradeType},
                #{quantity},
                #{price},
                #{totalAmount},
                #{tradeTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTrade(Trade trade);
}